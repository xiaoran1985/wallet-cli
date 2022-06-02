package org.tron.walletserver.example;

import java.io.IOException;

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
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.Common;
import org.tron.walletserver.GrpcClient;
import org.tron.walletserver.WalletApi;

/**
 * Created by Lidonglei on 2022/6/1.
 */
@SuppressWarnings("Duplicates")
public class BalanceContractExample {

  private static GrpcClient rpcCli = WalletApi.init();

  public static void main(String[] args) {
    BalanceContractExample example = new BalanceContractExample();
    String ownerAddress = "TCjuQbm5yab7ENTYb7tbdAKaiNa9Lrj4mo";
    //SR : TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F
    //其他：TCjuQbm5yab7ENTYb7tbdAKaiNa9Lrj4mo
    try {
      //freeze balance
//      boolean isFreeze = example.freezeBalance(WalletApi.decodeFromBase58Check(ownerAddress));
//      System.out.println("freeze result:"+isFreeze);
      //todo 冻结了三天
//      boolean isUnFreeze = example.unFreezeBalance(WalletApi.decodeFromBase58Check(ownerAddress));
//      System.out.println("unfreeze result:"+isUnFreeze);

      boolean isWithdraw = example.withdrawBalance();
      System.out.println("withdraw result:"+isWithdraw);

    } catch (Exception e) {
      System.out.println("method execute failed. msg:" + e);
    }
    System.out.println("balance execute done");
  }

  /**
   * freeze balance
   * @param owner
   * @return
   * @throws Exception
   */
  public boolean freezeBalance(byte[] owner) throws Exception {
    BalanceContract.FreezeBalanceContract contract = generateFreezeContract(owner);
    //invoke
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.createTransaction2(contract);
    //verify transaction
    if (!validTransaction(transactionExtention)) {
      System.out.println("do not success");
      return false;
    }
    return signAndBroadcast(transactionExtention.getTransaction());
  }


  private BalanceContract.FreezeBalanceContract generateFreezeContract(byte[] owner){
    BalanceContract.FreezeBalanceContract.Builder builder = BalanceContract.FreezeBalanceContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setFrozenBalance(10000000);//sun
    builder.setFrozenDuration(3);//是质押天数，最少是3天
    builder.setResource(Common.ResourceCode.ENERGY);
    builder.setReceiverAddress(ByteString.copyFrom(WalletApi.decodeFromBase58Check("TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F")));
    return builder.build();
  }

  /**
   *  unFreeze balance
   * @param owner
   * @return
   * @throws Exception
   */
  public boolean unFreezeBalance(byte[] owner) throws Exception {
    BalanceContract.UnfreezeBalanceContract contract = generateUnFreezeContract(owner);
    //invoke
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.createTransaction2(contract);
    //verify transaction
    if (!validTransaction(transactionExtention)) {
      System.out.println("do not success");
      return false;
    }
    return signAndBroadcast(transactionExtention.getTransaction());
  }

  private BalanceContract.UnfreezeBalanceContract generateUnFreezeContract(byte[] owner){
    BalanceContract.UnfreezeBalanceContract.Builder builder = BalanceContract.UnfreezeBalanceContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setReceiverAddress(ByteString.copyFrom(WalletApi.decodeFromBase58Check("TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F")));
    builder.setResource(Common.ResourceCode.ENERGY);
    return builder.build();
  }

  /**
   *  withdraw balance
   * @param
   * @return
   * @throws Exception
   */
  public boolean withdrawBalance() throws Exception {
    BalanceContract.WithdrawBalanceContract contract = generateWithdrawContract();
    //invoke
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.createTransaction2(contract);
    //verify transaction
    if (!validTransaction(transactionExtention)) {
      System.out.println("do not success");
      return false;
    }
    return signAndBroadcast(transactionExtention.getTransaction());
  }

  /**
   * withdraw contract
   * @param
   * @return
   */
  private BalanceContract.WithdrawBalanceContract generateWithdrawContract(){
    BalanceContract.WithdrawBalanceContract.Builder builder = BalanceContract.WithdrawBalanceContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(WalletApi.decodeFromBase58Check("TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F")));
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
