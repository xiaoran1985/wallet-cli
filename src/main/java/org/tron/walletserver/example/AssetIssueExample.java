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
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.walletserver.GrpcClient;
import org.tron.walletserver.WalletApi;

/**
 * Created by Lidonglei on 2022/6/1.
 */
@SuppressWarnings("Duplicates")
public class AssetIssueExample {
  private static GrpcClient rpcCli = WalletApi.init();

  public static void main(String[] args) {
    AssetIssueExample example = new AssetIssueExample();
    String ownerAddress = "TCjuQbm5yab7ENTYb7tbdAKaiNa9Lrj4mo";
    try {
      //create asset invoke
//      boolean isPublish = example.publishAssetIssue(WalletApi.decodeFromBase58Check(ownerAddress));
//      System.out.println("Publish result:" + isPublish);
      //buy asset invoke
      boolean isBuy = example.buyAssetIssue(WalletApi.decodeFromBase58Check(ownerAddress));
      System.out.println("buy result:" + isBuy);

    } catch (Exception e) {
      System.out.println("method execute failed. msg:" + e);
    }
    System.out.println("asset issue execute done");
  }

  /**
   * create asset issue
   * @param owner
   * @return
   * @throws Exception
   */
  public boolean publishAssetIssue(byte[] owner) throws Exception {
    AssetIssueContractOuterClass.AssetIssueContract contract = generateAssetIssueContract(owner);
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.createAssetIssue2(contract);
    if(!validTransaction(transactionExtention)){
      System.out.println("create asset result failed");
      return false;
    }
    System.out.println("transaction result:" + transactionExtention.getResult());

    return signAndBroadcast(transactionExtention.getTransaction());
  }

  /**
   * generate asset issue
   * @param owner
   * @return
   */
  private AssetIssueContractOuterClass.AssetIssueContract generateAssetIssueContract(byte[] owner){
    AssetIssueContractOuterClass.AssetIssueContract.Builder builder = AssetIssueContractOuterClass.AssetIssueContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    //不能是中文
    builder.setName(ByteString.copyFrom("asset_issue_token_test_1".getBytes()));
    builder.setAbbr(ByteString.copyFrom("AIT1".getBytes()));
    builder.setTotalSupply(10000);

    AssetIssueContractOuterClass.AssetIssueContract.FrozenSupply.Builder frozenBuilder = AssetIssueContractOuterClass.AssetIssueContract.FrozenSupply.newBuilder();
    frozenBuilder.setFrozenAmount(100);
    frozenBuilder.setFrozenDays(5);
    builder.addFrozenSupply(frozenBuilder.build());

    AssetIssueContractOuterClass.AssetIssueContract.FrozenSupply.Builder frozenBuilder1 = AssetIssueContractOuterClass.AssetIssueContract.FrozenSupply.newBuilder();
    frozenBuilder1.setFrozenAmount(400);
    frozenBuilder1.setFrozenDays(5);
    builder.addFrozenSupply(frozenBuilder1.build());

    builder.setTrxNum(1000);
    builder.setNum(10000);
    builder.setStartTime(1654099200000L);
    builder.setEndTime(1656604800000L);
    builder.setDescription(ByteString.copyFrom("测试创建资产描述".getBytes()));
    builder.setUrl(ByteString.copyFrom("www.tronscan.io".getBytes()));
    builder.setFreeAssetNetLimit(100);
    builder.setPublicFreeAssetNetLimit(100000);

    return builder.build();
  }


  /**
   * buy asset issue
   * @param owner
   * @return
   * @throws Exception
   */
  public boolean buyAssetIssue(byte[] owner) throws Exception {
    AssetIssueContractOuterClass.ParticipateAssetIssueContract contract = generateParticipateAsset(owner);
    GrpcAPI.TransactionExtention transactionExtention = rpcCli.createParticipateAssetIssueTransaction2(contract);
    if(!validTransaction(transactionExtention)){
      System.out.println("buy asset failed");
      return false;
    }

    return signAndBroadcast(transactionExtention.getTransaction());
  }

  /**
   *
   * @param owner
   * @return
   */
  private AssetIssueContractOuterClass.ParticipateAssetIssueContract generateParticipateAsset(byte[] owner){
    AssetIssueContractOuterClass.ParticipateAssetIssueContract.Builder builder = AssetIssueContractOuterClass.ParticipateAssetIssueContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setToAddress(ByteString.copyFrom(WalletApi.decodeFromBase58Check("TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F")));
    builder.setAmount(1000000);//sun
    builder.setAssetName(ByteString.copyFrom("asset_issue_token_test_1".getBytes()));
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
