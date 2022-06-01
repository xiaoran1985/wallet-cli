package org.tron.walletserver.example;

import java.io.IOException;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.Sha256Sm3Hash;
import org.tron.common.utils.AbiUtil;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.TransactionUtils;
import org.tron.common.utils.Utils;
import org.tron.core.exception.CancelException;
import org.tron.core.exception.CipherException;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import org.tron.walletserver.GrpcClient;
import org.tron.walletserver.WalletApi;

/**
 * Created by Lidonglei on 2022/5/31.
 */
@Slf4j
@SuppressWarnings("Duplicates")
public class SmartContractExample {

  private static GrpcClient rpcCli = WalletApi.init();

  public static void main(String[] args) {
    String abiStr = "[{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"a\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"b\",\"type\":\"uint256\"}],\"name\":\"sum\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"pure\",\"type\":\"function\"}]";
    String byteCode = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b5060f3806100396000396000f3fe6080604052348015600f57600080fd5b50d38015601b57600080fd5b50d28015602757600080fd5b506004361060405760003560e01c8063cad0899b146045575b600080fd5b605460503660046077565b6066565b60405190815260200160405180910390f35b6000607082846098565b9392505050565b60008060408385031215608957600080fd5b50508035926020909101359150565b6000821982111560b857634e487b7160e01b600052601160045260246000fd5b50019056fea26474726f6e582212200b55bd08bf4dd3bfd4f5a533d25a49591ffa2f7a3adf06f280532e5a9566437e64736f6c63430008060033";

    SmartContractExample example = new SmartContractExample();
    try {
      //deploy smart contract
//      boolean isDeployed = example.deployContract(abiStr, byteCode);
//      System.out.println("deploy result: " + isDeployed);
      //trigger smart contract
      boolean trigger = example.triggerSumContract();
      System.out.println("trigger result: " + trigger);
    } catch (Exception e) {
      System.out.println("failed. msg: " + e);
    }
  }

  /**
   * deploy smart contract
   *
   * @param abiStr
   * @param byteCodeStr
   * @return
   * @throws Exception
   */
  public boolean deployContract(String abiStr, String byteCodeStr) throws Exception {
    byte[] owner = WalletApi.decodeFromBase58Check("TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F");
    SmartContractOuterClass.CreateSmartContract createSmartContract = createSmartContract(owner, abiStr, byteCodeStr);

    //call grpc deploy
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.deployContract(createSmartContract);
    //verify return result
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("deploy failed");
      return false;
    }
    //generate broadcast transaction
    GrpcAPI.TransactionExtention.Builder texBuilder = GrpcAPI.TransactionExtention.newBuilder();
    Protocol.Transaction.Builder transBuilder = Protocol.Transaction.newBuilder();
    Protocol.Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(100000000l);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Protocol.Transaction.Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();
    //sign and print log and broadcast
    Protocol.Transaction transaction = transactionExtention.getTransaction();

    //print smart contract address
    byte[] contractAddress = generateContractAddress(owner,transaction);
    System.out.println("contract address:" + WalletApi.encode58Check(contractAddress));

    return signAndBroadcast(transaction);
  }

  private byte[] generateContractAddress(byte[] ownerAddress, Protocol.Transaction trx) {
    // get tx hash
    byte[] txRawDataHash = Sha256Sm3Hash.of(trx.getRawData().toByteArray()).getBytes();

    // combine
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);
  }

  private boolean signAndBroadcast(Protocol.Transaction transaction) throws
          CipherException, IOException, CancelException {
    //print log
    System.out.println(Utils.printTransactionExceptId(transaction));
    System.out.println("before sign transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    //sign
    transaction = signTransaction(transaction);

    System.out.println("after sign transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    System.out.println("txid is " + ByteArray.toHexString(Sha256Sm3Hash.hash(transaction.getRawData().toByteArray())));
    System.out.println(Utils.printTransactionExceptId(transaction));
    //call
    return rpcCli.broadcastTransaction(transaction);
  }

  /**
   * sign
   *
   * @param transaction
   * @return
   * @throws CipherException
   * @throws IOException
   * @throws CancelException
   */
  private Protocol.Transaction signTransaction(Protocol.Transaction transaction)
          throws CipherException, IOException, CancelException {
    if (transaction.getRawData().getTimestamp() == 0) {
      transaction = TransactionUtils.setTimestamp(transaction);
    }
    transaction = TransactionUtils.setExpirationTime(transaction);

    transaction = TransactionUtils.sign(transaction, getEcKey());

    return transaction;
  }

  /**
   * get private key
   *
   * @return
   * @throws CipherException
   */
  private ECKey getEcKey() throws IOException, CipherException {
    String priK = "";//clear private key
    byte[] privateKey = ByteArray.fromHexString(priK);
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    return ecKey;
  }

  /**
   * create smart contract obj
   *
   * @param owner       owner address
   * @param abiStr      abi(from tronscan)
   * @param byteCodeStr byteCode(from tronscan)
   * @return
   */
  private SmartContractOuterClass.CreateSmartContract createSmartContract(byte[] owner, String abiStr, String byteCodeStr) {
    SmartContractOuterClass.SmartContract.ABI abi = WalletApi.jsonStr2ABI(abiStr);

    SmartContractOuterClass.SmartContract.Builder contractBuilder = SmartContractOuterClass.SmartContract.newBuilder();
    contractBuilder.setAbi(abi);

    byte[] byteCodes = Hex.decode(byteCodeStr);

    contractBuilder.setBytecode(ByteString.copyFrom(byteCodes));
    contractBuilder.setName("tran-contract-test-sum");
    contractBuilder.setOriginAddress(ByteString.copyFrom(owner));
    contractBuilder.setConsumeUserResourcePercent(90L);
    contractBuilder.setOriginEnergyLimit(2L);

    SmartContractOuterClass.CreateSmartContract.Builder createSmartContractBuilder = SmartContractOuterClass.CreateSmartContract.newBuilder();
    createSmartContractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    createSmartContractBuilder.setNewContract(contractBuilder.build());
    createSmartContractBuilder.setTokenId(0L);
    createSmartContractBuilder.setCallTokenValue(0L);
    return createSmartContractBuilder.build();
  }

  /**
   * trigger contract
   *
   * @return
   * @throws Exception
   */
  public boolean triggerSumContract() throws Exception {
    String contractAddr = "TDR2QbmiN1JqjuK7SvXBrEssHsMThYj4Uu";
    byte[] contractAddress = WalletApi.decodeFromBase58Check(contractAddr);
    byte[] owner = WalletApi.decodeFromBase58Check("TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F");

    String methodDesc = "sum(uint256,uint256)";

    byte[] input = Hex.decode(AbiUtil.parseMethod(methodDesc, "10,20", false));
    System.out.println("input:" + input);
    SmartContractOuterClass.TriggerSmartContract triggerSmartContract = generateTriggerContract(owner, contractAddress, input);

    GrpcAPI.TransactionExtention transactionExtention = rpcCli.triggerContract(triggerSmartContract);

    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call trx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return false;
    }

    //generate broadcast transaction
    final GrpcAPI.TransactionExtention.Builder texBuilder = GrpcAPI.TransactionExtention.newBuilder();
    Protocol.Transaction.Builder transBuilder = Protocol.Transaction.newBuilder();
    Protocol.Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(2000000);

    transBuilder.setRawData(rawBuilder);

    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());

    transactionExtention = texBuilder.build();

    //sign and print log and broadcast
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    //txId : ByteArray.toHexString(Sha256Sm3Hash.hash(transaction.getRawData().toByteArray()));
    //print result
    String result = transactionExtention.getResult().getMessage().toStringUtf8();
    System.out.println("sum result:" + result);
    return signAndBroadcast(transaction);
  }

  /**
   * create trigger contract
   *
   * @param owner
   * @param contractAddress
   * @param input
   * @return
   */
  private SmartContractOuterClass.TriggerSmartContract generateTriggerContract(byte[] owner, byte[] contractAddress, byte[] input) {
    SmartContractOuterClass.TriggerSmartContract.Builder builder = SmartContractOuterClass.TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    return builder.build();
  }
}
