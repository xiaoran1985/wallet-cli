package org.tron.walletserver;

import java.io.File;
import java.io.IOException;

import com.google.protobuf.ByteString;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.api.GrpcAPI;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Sha256Sm3Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.TransactionUtils;
import org.tron.common.utils.Utils;
import org.tron.core.exception.CancelException;
import org.tron.core.exception.CipherException;
import org.tron.keystore.WalletFile;
import org.tron.keystore.WalletUtils;
import org.tron.protos.Protocol;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;

/**
 * Created by Lidonglei on 2022/5/30.
 */
@Slf4j
public class TransactionExample {

  private static GrpcClient rpcCli = null;

  static {
    rpcCli = WalletApi.init();
  }


  public static void main(String[] args) {

    TransactionExample example = new TransactionExample();

    String toAddress = "TCjuQbm5yab7ENTYb7tbdAKaiNa9Lrj4mo";
    String ownerAddress = "TTWJb3xRZr7iNRKku4a7aUX2QgmAT7o36F";

    try {
      boolean result = example.transferTrx(ownerAddress, toAddress, 10000000L);
      System.out.println("转账trx结果:" + result);
    } catch (Exception e) {
      System.out.println("error:" + e);
    }
    System.out.println("done");

  }

  /**
   * 转账trx
   * @param ownerAddress  原地址
   * @param toAddress     目标地址
   * @param amount        转账金额
   * @return
   */
  public boolean transferTrx(String ownerAddress,String toAddress,Long amount) throws Exception{
    //数据校验
    if(StringUtil.isNullOrEmpty(ownerAddress) || StringUtil.isNullOrEmpty(toAddress)){
      logger.error("转账地址错误");
      return false;
    }
    //转账金额判断？最小单位是sun，1trx = 1000000sun

    //地址转换为base58格式
    byte[] owner = WalletApi.decodeFromBase58Check(ownerAddress);
    byte[] to = WalletApi.decodeFromBase58Check(toAddress);
    //生成合约
    BalanceContract.TransferContract contract = createContract(owner,to,amount);
    //配置文件中版本是2，使用对应方法转账
    GrpcAPI.TransactionExtention extention = rpcCli.createTransaction2(contract);
    //验证返回结果
    boolean validResult = validTransaction(extention);
    if(validResult == false){
      logger.error("转账结果验证失败");
      return validResult;
    }
    Protocol.Transaction transaction = extention.getTransaction();
    //签名并打印日志
    signAndPrint(transaction);
    //广播
    return rpcCli.broadcastTransaction(transaction);
  }

  private void signAndPrint(Protocol.Transaction transaction) throws
          CipherException, IOException, CancelException{
    //打印
    System.out.println(Utils.printTransactionExceptId(transaction));
    System.out.println("before sign transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    //签名
    transaction = signTransaction(transaction);
    //打印
    System.out.println("after sign transaction hex string is " +
            ByteArray.toHexString(transaction.toByteArray()));
    System.out.println("txid is " +
            ByteArray.toHexString(Sha256Sm3Hash.hash(transaction.getRawData().toByteArray())));

  }

  /**
   * 创建合约
   * @param owner   所有方
   * @param to      接收方
   * @param amount  金额，单位sun
   * @return
   */
  private BalanceContract.TransferContract createContract(byte[] owner,byte[] to,long amount){
    BalanceContract.TransferContract.Builder builder = BalanceContract.TransferContract.newBuilder();
    //所有人地址
    ByteString ownerString = ByteString.copyFrom(owner);
    builder.setOwnerAddress(ownerString);
    //目标地址
    ByteString toString = ByteString.copyFrom(to);
    builder.setToAddress(toString);
    //转账数量
    builder.setAmount(amount);

    return builder.build();
  }

  /**
   * 验证返回结果
   * @param transaction
   * @return
   */
  private boolean validTransaction(GrpcAPI.TransactionExtention transaction){
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
   * 签名
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
   * 获取私钥
   * @return
   * @throws CipherException
   */
  private ECKey getEcKey() throws IOException,CipherException {
    String priK = "";
    byte[] privateKey = ByteArray.fromHexString(priK);
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    return ecKey;
  }

  private WalletFile getWallet() throws IOException {
    String path = "/Users/pro/Downloads/walletFile";
    WalletFile file = WalletUtils.loadWalletFile(new File(path));
    return file;
  }

  /**
   * 转账TRC10
   * @param ownerAddr
   * @param toAddr
   * @param assetName
   * @param amount
   * @return
   * @throws Exception
   */
  public boolean transferTrc10(String ownerAddr,String toAddr,String assetName,Long amount)
          throws Exception {
    //地址转换为base58格式
    byte[] owner = WalletApi.decodeFromBase58Check(ownerAddr);
    byte[] to = WalletApi.decodeFromBase58Check(toAddr);
    AssetIssueContractOuterClass.TransferAssetContract assetContract = createTransferAssetContract(owner,to,assetName,amount);

    //配置版本是2，使用对应方法转账
    GrpcAPI.TransactionExtention extention = rpcCli.createTransferAssetTransaction2(assetContract);

    //验证返回结果
    boolean validResult = validTransaction(extention);
    if(validResult == false){
      logger.error("转账结果验证失败");
      return validResult;
    }
    Protocol.Transaction transaction = extention.getTransaction();
    //签名并打印日志
    signAndPrint(transaction);
    //广播
    return rpcCli.broadcastTransaction(transaction);
  }

  /**
   * 生成合约
   * @param owner
   * @param to
   * @param assetName
   * @param amount
   * @return
   */
  private AssetIssueContractOuterClass.TransferAssetContract createTransferAssetContract(byte[] owner,
            byte[] to,String assetName,Long amount){
    AssetIssueContractOuterClass.TransferAssetContract.Builder builder = AssetIssueContractOuterClass.TransferAssetContract.newBuilder();
    builder.setAmount(amount);
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setToAddress(ByteString.copyFrom(to));
    builder.setAssetName(ByteString.copyFrom(assetName.getBytes()));
    return builder.build();
  }

}
