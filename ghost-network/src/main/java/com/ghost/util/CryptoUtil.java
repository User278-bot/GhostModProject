package com.ghost.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.ByteBuffer;

public final class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtil() {}

    private static SecretKeySpec getSecretKey(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static String encrypt(String plainText, String password) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(password), parameterSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);
        byte[] cipherMessage = byteBuffer.array();

        return Base64.getEncoder().encodeToString(cipherMessage);
    }

    public static String decrypt(String cipherMessageBase64, String password) throws Exception {
        byte[] cipherMessage = Base64.getDecoder().decode(cipherMessageBase64);

        if (cipherMessage.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid cipher message length");
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(cipherMessage, 0, iv, 0, GCM_IV_LENGTH);

        byte[] cipherText = new byte[cipherMessage.length - GCM_IV_LENGTH];
        System.arraycopy(cipherMessage, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(password), parameterSpec);

        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, StandardCharsets.UTF_8);
    }
}
