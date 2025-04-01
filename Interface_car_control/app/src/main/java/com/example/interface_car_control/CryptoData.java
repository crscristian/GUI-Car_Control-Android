package com.example.interface_car_control;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;
import android.util.Log;

public class CryptoData {
    public static String key = "N3XBlR7W2qSwWcux9ZHJTpXJdaNzdVLlhjLyyPvwS60=";

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    public static String encrypt(String input) throws Exception {
        Log.i("Decrypted command: ", input);
        SecretKeySpec secretKey = new SecretKeySpec(Base64.decode(key, Base64.DEFAULT), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(input.getBytes("UTF-8"));
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    public static String decrypt(String input) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(Base64.decode(key, Base64.DEFAULT), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.decode(input, Base64.DEFAULT));
        return new String(decryptedBytes, "UTF-8");
    }

}
