package org.tron.walletserver;

import java.io.IOException;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.tron.api.GrpcAPI;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Sha256Sm3Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.TransactionUtils;
import org.tron.common.utils.Utils;
import org.tron.core.config.Parameter;
import org.tron.core.exception.CancelException;
import org.tron.core.exception.CipherException;
import org.tron.protos.Protocol;
import org.tron.protos.contract.AccountContract;

/**
 * Created by Lidonglei on 2022/6/1.
 */
@Slf4j
@SuppressWarnings("Duplicates")
public class ContractExample {
  private static GrpcClient rpcCli = WalletApi.init();


  public static void main(String[] args) {
    WalletApi.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ContractExample example = new ContractExample();
    String ownerAddress = "TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F";
    try{
      //create account
      boolean isCreated = example.createAccount(WalletApi.decodeFromBase58Check(ownerAddress));
      System.out.println("create account result:" + isCreated);
    }catch (Exception e){
      System.out.println("method execute failed. msg:" + e);
    }
    System.out.println("method done");
  }

  public boolean createAccount(byte[] owner) throws Exception {
    GrpcAPI.EmptyMessage.Builder builder = GrpcAPI.EmptyMessage.newBuilder();
    GrpcAPI.AddressPrKeyPairMessage pairMessage = rpcCli.generateAddress(builder.build());
    System.out.println("pairMessage:" + pairMessage);
    byte[] account = WalletApi.decodeFromBase58Check(pairMessage.getAddress());
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.createAccount2(generateAccountContract(owner,account));

    boolean valid = validTransaction(transactionExtention);
    if(!valid){
      System.out.println("create account do not valid pass");
      return false;
    }
    System.out.println("account address:" + pairMessage.getAddress());
    return signAndBroadcast(transactionExtention.getTransaction());
  }

  private AccountContract.AccountCreateContract generateAccountContract(byte[] owner,byte[] account){
    AccountContract.AccountCreateContract.Builder builder = AccountContract.AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(account));
    builder.setType(Protocol.AccountType.Normal);
    return builder.build();
  }

  /**
   *
   * @param transaction
   * @return
   * @throws CipherException
   * @throws IOException
   * @throws CancelException
   */
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
   * 验证返回结果
   *
   * @param transaction
   * @return
   */
  private boolean validTransaction(GrpcAPI.TransactionExtention transaction) {
    if (transaction == null) {
      return false;
    }
    GrpcAPI.Return ret = transaction.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Protocol.Transaction trans = transaction.getTransaction();
    if (trans == null || trans.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }

    if (trans.getRawData().getContract(0).getType()
            == Protocol.Transaction.Contract.ContractType.ShieldedTransferContract) {
      return false;
    }
    return true;
  }

  /**
   * sign transaction
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
   * get eckey
   *
   * @return
   * @throws CipherException
   */
  private ECKey getEcKey() throws IOException, CipherException {
    String priK = "1edbbc868af9ac01260e9322c340c1d6ddfc1db972a6e56cb4c4cbf98bc4c4da";
    byte[] privateKey = ByteArray.fromHexString(priK);
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    return ecKey;
  }

}
