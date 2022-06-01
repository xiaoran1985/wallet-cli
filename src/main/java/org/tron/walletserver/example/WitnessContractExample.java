package org.tron.walletserver.example;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.ByteString;
import org.tron.api.GrpcAPI;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Sha256Sm3Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.TransactionUtils;
import org.tron.common.utils.Utils;
import org.tron.core.exception.CancelException;
import org.tron.core.exception.CipherException;
import org.tron.protos.Protocol;
import org.tron.protos.contract.WitnessContract;
import org.tron.walletserver.GrpcClient;
import org.tron.walletserver.WalletApi;

/**
 * Created by Lidonglei on 2022/6/1.
 */
@SuppressWarnings("Duplicates")
public class WitnessContractExample {

  private static GrpcClient rpcCli = WalletApi.init();

  public static void main(String[] args) {
    WitnessContractExample example = new WitnessContractExample();
    String ownerAddress = "TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F";
    try {
      //vote invoke
//      boolean isVote = example.vote(WalletApi.decodeFromBase58Check(ownerAddress));
//      System.out.println("Vote result:" + isVote);
      //create witness
//      boolean isCreateWitness = example.createWitness(WalletApi.decodeFromBase58Check(ownerAddress));
//      System.out.println("create witness result:" + isCreateWitness);
      //update witness
      boolean isUpdate = example.updateWitness(WalletApi.decodeFromBase58Check(ownerAddress),"www.tronscan.io");
      System.out.println("update witness result:" + isUpdate);
    } catch (Exception e) {
      System.out.println("method execute failed. msg:" + e);
    }
    System.out.println("witness execute done");
  }

  /**
   * vote sr
   * @param owner
   * @return
   * @throws Exception
   */
  public boolean vote(byte[] owner) throws Exception {
    //init vote
    HashMap<String, Long> voteMap = new HashMap<>();
    voteMap.put("TPffmvjxEcvZefQqS7QYvL1Der3uiguikE", 3L);
    voteMap.put("TFFLWM7tmKiwGtbh2mcz2rBssoFjHjSShG", 1L);
    WitnessContract.VoteWitnessContract contract = generateVoteContract(owner, voteMap);
    //invoke
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.voteWitnessAccount2(contract);
    //verify transaction
    if (!validTransaction(transactionExtention)) {
      System.out.println("do not success");
      return false;
    }
    System.out.println("transaction result:" + transactionExtention.getResult());
    return signAndBroadcast(transactionExtention.getTransaction());
  }

  /**
   * generate vote contract
   * @param owner
   * @param voteMap
   * @return
   */
  private WitnessContract.VoteWitnessContract generateVoteContract(byte[] owner, HashMap<String, Long> voteMap) {
    WitnessContract.VoteWitnessContract.Builder builder = WitnessContract.VoteWitnessContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    //key:address,value:vote count
    for (Map.Entry<String, Long> entry : voteMap.entrySet()) {
      WitnessContract.VoteWitnessContract.Vote.Builder voteBuilder = WitnessContract.VoteWitnessContract.Vote.newBuilder();
      //do not verify address,because default value exist
      voteBuilder.setVoteAddress(ByteString.copyFrom(WalletApi.decodeFromBase58Check(entry.getKey())));
      voteBuilder.setVoteCount(entry.getValue());
      builder.addVotes(voteBuilder.build());
    }
    return builder.build();
  }


  /**
   * create sr
   * @param owner
   * @return
   */
  public boolean createWitness(byte[] owner) throws Exception {
    //create contract
    WitnessContract.WitnessCreateContract contract = generateWitnessContract(owner,"www.baidu.com");
    //invoke rpc
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.createWitness2(contract);
    //verify
    if(!validTransaction(transactionExtention)){
      System.out.println("valid failed");
      return false;
    }
    //print transaction
    System.out.println("transactionExtention : " + transactionExtention);
    return signAndBroadcast(transactionExtention.getTransaction());
  }

  private WitnessContract.WitnessCreateContract generateWitnessContract(byte[] owner,String url){
    WitnessContract.WitnessCreateContract.Builder builder = WitnessContract.WitnessCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(url.getBytes()));
    return builder.build();
  }

  /**
   * update witness
   * @param owner
   * @param url
   * @return
   */
  public boolean updateWitness(byte[] owner,String url) throws Exception {
    WitnessContract.WitnessUpdateContract contract = generateUpdateContract(owner, url);
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.updateWitness2(contract);
    if(!validTransaction(transactionExtention)){
      System.out.println("update witness failed");
      return false;
    }
    return signAndBroadcast(transactionExtention.getTransaction());
  }

  private WitnessContract.WitnessUpdateContract generateUpdateContract(byte[] owner,String url){
    WitnessContract.WitnessUpdateContract.Builder builder = WitnessContract.WitnessUpdateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUpdateUrl(ByteString.copyFrom(url.getBytes()));
    return builder.build();
  }

  /**
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
   * verify transaction
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
    String priK = "";
    byte[] privateKey = ByteArray.fromHexString(priK);
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    return ecKey;
  }
}
