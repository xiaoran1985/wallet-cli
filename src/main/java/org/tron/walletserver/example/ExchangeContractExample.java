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
import org.tron.protos.contract.ExchangeContract;
import org.tron.walletserver.GrpcClient;
import org.tron.walletserver.WalletApi;

/**
 * Created by Lidonglei on 2022/6/2.
 */
@SuppressWarnings("Duplicates")
public class ExchangeContractExample {

  private static GrpcClient rpcCli = WalletApi.init();

  public static void main(String[] args) {
    ExchangeContractExample example = new ExchangeContractExample();
    String ownerAddress = "TCjuQbm5yab7ENTYb7tbdAKaiNa9Lrj4mo";
    try {
      //create
//      boolean isCreate = example.createExchange(WalletApi.decodeFromBase58Check(ownerAddress));
//      System.out.println("create result:" + isCreate);
      //inject
//      boolean isInject = example.injectExchange(WalletApi.decodeFromBase58Check(ownerAddress));
//      System.out.println("inject result:" + isInject);
      //withdraw
//      boolean isWithdraw = example.withdrawExchange(WalletApi.decodeFromBase58Check(ownerAddress));
//      System.out.println("withdraw result:" + isWithdraw);
      //transaction
      boolean isTransaction = example.transactionExchange();
      System.out.println("transaction result:" + isTransaction);
    } catch (Exception e) {
      System.out.println("method execute failed. msg:" + e);
    }
    System.out.println("exchange execute done");
  }

  /**
   * create exchange
   * @param owner
   * @return
   * @throws Exception
   */
  public boolean createExchange(byte[] owner) throws Exception {
    ExchangeContract.ExchangeCreateContract contract = generateCreateContract(owner);
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.exchangeCreate(contract);
    if(!validTransaction(transactionExtention)){
      System.out.println("create exchange result failed");
      return false;
    }
    return signAndBroadcast(transactionExtention.getTransaction());
  }

  /**
   * inject exchange
   * @param owner
   * @return
   * @throws Exception
   */
  public boolean injectExchange(byte[] owner) throws Exception {
    ExchangeContract.ExchangeInjectContract contract = generateInjectContract(owner);
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.exchangeInject(contract);
    if(!validTransaction(transactionExtention)){
      System.out.println("inject exchange result failed");
      return false;
    }
    return signAndBroadcast(transactionExtention.getTransaction());
  }

  private ExchangeContract.ExchangeCreateContract generateCreateContract(byte[] owner){
    ExchangeContract.ExchangeCreateContract.Builder builder = ExchangeContract.ExchangeCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setFirstTokenId(ByteString.copyFrom("1004852".getBytes()));
    builder.setFirstTokenBalance(100);
    builder.setSecondTokenId(ByteString.copyFrom("_".getBytes()));
    builder.setSecondTokenBalance(100000000);
    return builder.build();
  }


  private ExchangeContract.ExchangeInjectContract generateInjectContract(byte[] owner){
    ExchangeContract.ExchangeInjectContract.Builder builder = ExchangeContract.ExchangeInjectContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setTokenId(ByteString.copyFrom("1004852".getBytes()));
    builder.setQuant(10);
    builder.setExchangeId(14);
    return builder.build();
  }

  /**
   * withdraw exchange
   * @param owner
   * @return
   * @throws Exception
   */
  public boolean withdrawExchange(byte[] owner) throws Exception {
    ExchangeContract.ExchangeWithdrawContract contract = generateWithdrawContract(owner);
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.exchangeWithdraw(contract);
    if(!validTransaction(transactionExtention)){
      System.out.println("withdraw exchange result failed");
      return false;
    }
    return signAndBroadcast(transactionExtention.getTransaction());
  }

  private ExchangeContract.ExchangeWithdrawContract generateWithdrawContract(byte[] owner){
    ExchangeContract.ExchangeWithdrawContract.Builder builder = ExchangeContract.ExchangeWithdrawContract.newBuilder();
    builder.setExchangeId(14);
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setTokenId(ByteString.copyFrom("1004852".getBytes()));
    builder.setQuant(10);
    return builder.build();
  }

  /**
   * transaction exchange
   * @param
   * @return
   * @throws Exception
   */
  public boolean transactionExchange() throws Exception {
    ExchangeContract.ExchangeTransactionContract contract = generateTransactionContract(WalletApi.decodeFromBase58Check("TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F"));
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.exchangeTransaction(contract);
    if(!validTransaction(transactionExtention)){
      System.out.println("transaction exchange result failed");
      return false;
    }
    return signAndBroadcast(transactionExtention.getTransaction());
  }

  private ExchangeContract.ExchangeTransactionContract generateTransactionContract(byte[] owner){
    ExchangeContract.ExchangeTransactionContract.Builder builder = ExchangeContract.ExchangeTransactionContract.newBuilder();
    builder.setExchangeId(14);
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setTokenId(ByteString.copyFrom("_".getBytes()));
    builder.setQuant(10000000);//sun
    builder.setExpected(8);
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
