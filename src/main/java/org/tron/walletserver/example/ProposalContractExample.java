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
import org.tron.protos.contract.ProposalContract;
import org.tron.walletserver.GrpcClient;
import org.tron.walletserver.WalletApi;

/**
 * Created by Lidonglei on 2022/6/2.
 */
@SuppressWarnings("Duplicates")
public class ProposalContractExample {

  private static GrpcClient rpcCli = WalletApi.init();

  public static void main(String[] args) {
    ProposalContractExample example = new ProposalContractExample();
    String ownerAddress = "TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F";
    try {
      //create
//      boolean isCreate = example.createProposal(WalletApi.decodeFromBase58Check(ownerAddress));
//      System.out.println("create result:" + isCreate);
      //approve
//      boolean isApprove = example.approveProposal(WalletApi.decodeFromBase58Check(ownerAddress));
//      System.out.println("approve result:" + isApprove);
      //delete
      boolean isDelete = example.deleteProposal(WalletApi.decodeFromBase58Check(ownerAddress));
      System.out.println("delete result:" + isDelete);
    } catch (Exception e) {
      System.out.println("method execute failed. msg:" + e);
    }
    System.out.println("proposal issue execute done");
  }

  /**
   * create proposal
   * @param owner
   * @return
   * @throws Exception
   */
  public boolean createProposal(byte[] owner) throws Exception {
    ProposalContract.ProposalCreateContract contract = generateProposalContract(owner);
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.proposalCreate(contract);
    if(!validTransaction(transactionExtention)){
      System.out.println("create proposal result failed");
      return false;
    }
    System.out.println("transaction result:" + transactionExtention.getResult());

    return signAndBroadcast(transactionExtention.getTransaction());
  }

  /**
   * approve proposal
   * @param owner
   * @return
   * @throws Exception
   */
  public boolean approveProposal(byte[] owner) throws Exception {
    ProposalContract.ProposalApproveContract contract = generateApproveContract(owner);
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.proposalApprove(contract);
    if(!validTransaction(transactionExtention)){
      System.out.println("create proposal result failed");
      return false;
    }
    System.out.println("transaction result:" + transactionExtention.getResult());

    return signAndBroadcast(transactionExtention.getTransaction());
  }

  private ProposalContract.ProposalCreateContract generateProposalContract(byte[] owner){
    Map<Long,Long> params = new HashMap<>();
    params.put(1L,10000000L);//提议修改账户升级为超级代表的费用
    ProposalContract.ProposalCreateContract.Builder builder = ProposalContract.ProposalCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.putAllParameters(params);
    return builder.build();
  }

  private ProposalContract.ProposalApproveContract generateApproveContract(byte[] owner){
    ProposalContract.ProposalApproveContract.Builder builder = ProposalContract.ProposalApproveContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(12406);
    builder.setIsAddApproval(true);
    return builder.build();
  }

  /**
   * delete proposal
   * @param owner
   * @return
   * @throws Exception
   */
  public boolean deleteProposal(byte[] owner) throws Exception {
    ProposalContract.ProposalDeleteContract contract = generateDeleteContract(owner);
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.proposalDelete(contract);
    if(!validTransaction(transactionExtention)){
      System.out.println("delete proposal result failed");
      return false;
    }
    System.out.println("transaction result:" + transactionExtention.getResult());

    return signAndBroadcast(transactionExtention.getTransaction());
  }

  private ProposalContract.ProposalDeleteContract generateDeleteContract(byte[] owner){
    ProposalContract.ProposalDeleteContract.Builder builder = ProposalContract.ProposalDeleteContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(12406);
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
