package com.rsa.utils;

import com.rsa.JdbcDemo;
import com.rsa.conf.DatabaseConfig;
import config.ConfigLoader;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TCompactProtocol;
import com.rsa.thrift.TBDPUserInfo;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AESUtil {
    private static final String ALGORITHM = "AES";

    public static final byte[] EMPTY_PASSWORD = new byte[0];
    public static final int SCRAMBLE_LENGTH = 20;
    public static final int SCRAMBLE_LENGTH_HEX_LENGTH = 2 * SCRAMBLE_LENGTH + 1;
    public static final byte PVERSION41_CHAR = '*';
    private static final byte[] DIG_VEC_UPPER = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final ConfigLoader configLoader = ConfigLoader.getInstance();
    public static DatabaseConfig dbConfig = configLoader.getLakehouseDBConfig(JdbcDemo.sourceName);
//    public static DatabaseConfig dbConfig = configLoader.getLakehouseDBConfig("gateway");

    // 字节数为16位
    private static final byte[] KEY = dbConfig.getCipherKey().getBytes(StandardCharsets.UTF_8);


    public static String encrypt(byte[] plainBytes) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedBytes = cipher.doFinal(plainBytes);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static TBDPUserInfo decrypt(String encryptedText) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        TDeserializer deserializer = new TDeserializer(new TCompactProtocol.Factory());
        TBDPUserInfo userInfo = new TBDPUserInfo();
        deserializer.deserialize(userInfo, decryptedBytes);
        return userInfo;
    }

    private static byte[] twoStageHash(String password) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] passBytes = password.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashStage1 = md.digest(passBytes);
        md.reset();
        byte[] hashStage2 = md.digest(hashStage1);
        return hashStage2;
    }

    public static byte[] makeScrambledPassword(String plainPasswd) throws UnsupportedEncodingException, NoSuchAlgorithmException {
//        if (StringUtils.isNullOrEmpty(plainPasswd)) {
//            return EMPTY_PASSWORD;
//        }
        byte[] hashStage2 = twoStageHash(plainPasswd);
        byte[] passwd = new byte[SCRAMBLE_LENGTH_HEX_LENGTH];
        passwd[0] = (PVERSION41_CHAR);
        octetToHexSafe(passwd, 1, hashStage2);
        return passwd;
    }

    // covert octet 'from' to hex 'to'
    // NOTE: this function assume that to buffer is enough
    private static void octetToHexSafe(byte[] to, int toOff, byte[] from) {
        int j = toOff;
        for (int i = 0; i < from.length; i++) {
            int val = from[i] & 0xff;
            to[j++] = DIG_VEC_UPPER[val >> 4];
            to[j++] = DIG_VEC_UPPER[val & 0x0f];
        }
    }


    public static String getCipherText() throws Exception {
        TBDPUserInfo userInfo = new TBDPUserInfo();
        userInfo.setErp("easyolap");
        userInfo.setHadoopUserName(dbConfig.getHadoopUserName()); // 可选
        userInfo.setSource(JdbcDemo.sourceName); // 必填
        userInfo.setUserToken(dbConfig.getHadoopUserToken());
//        userInfo.setUserToken("".equals(dbConfig.getHadoopUserToken())
//                ? getUserTokenFromIAMEndpoint("caokaihua1", dbConfig.getHadoopUserName())
//                : dbConfig.getHadoopUserToken()
//        );
        userInfo.setScrambledPassword(makeScrambledPassword(dbConfig.getPassword())); //必填
        userInfo.setCatalog("hive"); // 可选 ,单独设置catalog
//        userInfo.setDb("dms_dev");
//        userInfo.setDb("dev");

        // 个人开发者账号
//        userInfo.setUserType("dev_personal");
//        userInfo.setBusinessLine("jcw_dmf");

        // 创建 TSerializer 实例并指定使用 TCompactProtocol
        TSerializer serializer = new TSerializer(new TCompactProtocol.Factory());
        return encrypt(serializer.serialize(userInfo));
    }

    public static void main(String[] args) throws Exception {

        String cipherText = "/TrKtpFw/CLcb6AuTa99Ybw6U3ZABQ28ebKNRiv3P4hKf3G43jXL7WU6oBObefywu6G1drU2BGqmk6J0s63oiRebKzeAmH0Y6e518FAa95GIBw2Pu0K2SRm4D4uVY5s7VSw08M25S6VbVW6ZZXU1ucrHiMG53IDgsvBFkWNMAoibiDJ8M0GjkzVtxLKJ/s5lisZXfoHLKejqfGMln6hlHRGiJbRSMNqJxyNj7uJln4W0=";
        String cipherTextGenerated = getCipherText();
        if (cipherTextGenerated.equals(cipherText)) {
            System.out.println("the generated cipher is equivalent!");
        } else {
            System.out.println(cipherText);
            System.out.println("vs");
            System.out.println(cipherTextGenerated);
        }
        System.out.println("===== the cipher text =====");
        TBDPUserInfo decrypt = decrypt(cipherText);
        System.out.println(decrypt.source);
        System.out.println(decrypt.hadoopUserName);
        System.out.println(decrypt.userToken);
        System.out.println(decrypt.erp);
        System.out.println(decrypt.catalog);
        System.out.println(decrypt.db);
        System.out.println(decrypt.scrambledPassword);

        System.out.println("===== cipher text generated =====");
        TBDPUserInfo decrypt1 = decrypt(cipherTextGenerated);
        System.out.println("The source is: " + decrypt1.source);
        System.out.println(decrypt1.hadoopUserName);
        System.out.println(decrypt1.userToken);
    }
}

