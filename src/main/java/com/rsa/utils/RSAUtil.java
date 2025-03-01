package com.rsa.utils;

import com.rsa.thrift.TBDPUserInfo;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TCompactProtocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSAUtil {

    public static final String KEY_ALGORIHTM = "RSA";
    private static final int MAX_ENCRYPT_BLOCK = 117;
    private static final int MAX_DECRYPT_BLOCK = 128;

    public static Pair<RSAPublicKey, RSAPrivateKey> generateRSAKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORIHTM);
        keyPairGen.initialize(keySize);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return Pair.of(publicKey, privateKey);
    }

    public static String getPublicKeyStr(Pair<RSAPublicKey, RSAPrivateKey> keyPair) {
        return encryptBase64(keyPair.key().getEncoded());
    }

    public static String getPrivateKeyStr(Pair<RSAPublicKey, RSAPrivateKey> keyPair) {
        return encryptBase64(keyPair.value().getEncoded());
    }

    public static String encryptBase64(byte[] key) {
        return Base64.getEncoder().encodeToString(key);
    }

    public static byte[] decryptBase64(String key) {
        return Base64.getDecoder().decode(key);
    }

    public static PublicKey getPublicKey(String publicKeyStr)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] publicKeyByte = decryptBase64(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyByte);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORIHTM);
        return keyFactory.generatePublic(keySpec);
    }

    public static PrivateKey getPrivateKey(String privateKeyStr)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] privateKeyByte = decryptBase64(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyByte);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORIHTM);
        return keyFactory.generatePrivate(keySpec);
    }

    public static String encryptByPrivateKey(byte[] textBytes, String privateKeyStr)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, IOException {
        Cipher cipher = Cipher.getInstance(KEY_ALGORIHTM);
        cipher.init(Cipher.ENCRYPT_MODE, getPrivateKey(privateKeyStr));
        return encrypt(cipher, textBytes);
    }

    public static String encryptByPublicKey(byte[] textBytes, String publicKeyStr)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, IOException {
        Cipher cipher = Cipher.getInstance(KEY_ALGORIHTM);
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(publicKeyStr));
        return encrypt(cipher, textBytes);
    }

    private static String encrypt(Cipher cipher, byte[] textBytes)
            throws IllegalBlockSizeException, BadPaddingException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (int i = 0, offset = 0; textBytes.length - offset > 0; i++, offset = MAX_ENCRYPT_BLOCK * i) {
            byte[] tempBytes = textBytes.length - offset > MAX_ENCRYPT_BLOCK ?
                    cipher.doFinal(textBytes, offset, MAX_ENCRYPT_BLOCK) :
                    cipher.doFinal(textBytes, offset, textBytes.length - offset);
            byteArrayOutputStream.write(tempBytes);
        }
        byteArrayOutputStream.close();
        return encryptBase64(byteArrayOutputStream.toByteArray());
    }

    public static TBDPUserInfo decryptByPublicKey(String text, String publicKeyStr)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, IOException, TException {
        Cipher cipher = Cipher.getInstance(KEY_ALGORIHTM);
        cipher.init(Cipher.DECRYPT_MODE, getPublicKey(publicKeyStr));
        byte[] textBytes = decryptBase64(text);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (int i = 0, offset = 0; textBytes.length - offset > 0; i++, offset = MAX_DECRYPT_BLOCK * i) {
            byte[] tempBytes = textBytes.length - offset > MAX_DECRYPT_BLOCK ?
                    cipher.doFinal(textBytes, offset, MAX_DECRYPT_BLOCK) :
                    cipher.doFinal(textBytes, offset, textBytes.length - offset);
            byteArrayOutputStream.write(tempBytes);
        }
        byteArrayOutputStream.close();
        TDeserializer deserializer = new TDeserializer(new TCompactProtocol.Factory());
        TBDPUserInfo userInfo = new TBDPUserInfo();
        deserializer.deserialize(userInfo, byteArrayOutputStream.toByteArray());
        return userInfo;
    }

    public static String decryptByPrivateKey(String text, String privateKeyStr)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, IOException, TException {
        Cipher cipher = Cipher.getInstance(KEY_ALGORIHTM);
        cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(privateKeyStr));
        byte[] textBytes = decryptBase64(text);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (int i = 0, offset = 0; textBytes.length - offset > 0; i++, offset = MAX_DECRYPT_BLOCK * i) {
            byte[] tempBytes = textBytes.length - offset > MAX_DECRYPT_BLOCK ?
                    cipher.doFinal(textBytes, offset, MAX_DECRYPT_BLOCK) :
                    cipher.doFinal(textBytes, offset, textBytes.length - offset);
            byteArrayOutputStream.write(tempBytes);
        }
        byteArrayOutputStream.close();
        return byteArrayOutputStream.toString();
    }

    public static String getCipherText() throws TException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, IOException, InvalidKeyException {
        String privateKey = "your rsa private key";
        TBDPUserInfo userInfo = new TBDPUserInfo();
        userInfo.setErp("your erp");
        userInfo.setHadoopUserName("your bigdata username");
        userInfo.setSource("easy_olap");
        userInfo.setUserToken("your bigdata token");
        userInfo.setCatalog("hive");
        userInfo.setDb("dev");
        TSerializer serializer = new TSerializer();
        String cipherText = encryptByPrivateKey(serializer.serialize(userInfo), privateKey);
        return cipherText;
    }
}

