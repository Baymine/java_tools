package com.rsa.utils;

import com.rsa.thrift.TBDPUserInfo;
import org.apache.thrift.TException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static com.rsa.utils.RSAUtil.decryptByPublicKey;

public class Decrypt {
    public static void main(String[] args) throws NoSuchAlgorithmException, TException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeySpecException, BadPaddingException, IOException, InvalidKeyException {
        String cipherText = "TrKtpFw/CLcb6AuTa99Ybw6U3ZABQ28ebKNRiv3P4hKf3G43jXL7WU6oBObefywukMATRtQrOueYWniWU6iEqNnSDMg0E7Hu2PH0f+7Fu9+cW6j8vRph4fV+iag3iIqGsqeY3a54J4vqP/I+u5BYekdA/2qjY8Zn6SqZShFRKk9a8vPhXIG6Hy1TzZym6h6pJD+sxWFk9k8PwsQ3NItfWwlNBc6m7ii8zzn/xwakM8g=";
        String key = "gatewaydoriskey1";
        TBDPUserInfo userInfo1 = decryptByPublicKey(cipherText, key);

        System.out.println("cipher text : " + cipherText + " size : " + cipherText.length());
        System.out.println("erp : " + userInfo1.getErp());
        System.out.println("source: " + userInfo1.getSource());
        System.out.println("user token : " + userInfo1.getUserToken());
    }
}
