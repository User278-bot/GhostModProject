package com.ghost.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * AES-GCM を用いたバイナリパケットの暗号化・復号ユーティリティ。
 * セッション／サーバー単位で {@link #deriveKey(String)} の結果をキャッシュして使い回すことで、
 * 毎パケットの鍵導出コストを排除します。
 */
public final class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_TAG_BYTES = GCM_TAG_LENGTH / 8;
    private static final int MIN_CIPHER_MESSAGE_LENGTH = GCM_IV_LENGTH + GCM_TAG_BYTES;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtil() {
    }

    /**
     * パスワード文字列から AES-256 用の {@link SecretKey} を導出します。
     * セッション生成時に 1 回だけ呼び出し、戻り値を再利用してください。
     */
    public static SecretKey deriveKey(String password) {
        Objects.requireNonNull(password, "password must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 はすべての標準 JVM で必須実装
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }

    /**
     * 事前導出済みの鍵で平文バイト列を AES-GCM 暗号化し、[12B IV + 暗号文 + 16B タグ] を返します。
     */
    public static byte[] encrypt(byte[] plainBytes, SecretKey secretKey) throws GeneralSecurityException {
        Objects.requireNonNull(plainBytes, "plainBytes must not be null");
        Objects.requireNonNull(secretKey, "secretKey must not be null");

        byte[] iv = new byte[GCM_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] cipherText = cipher.doFinal(plainBytes);

        byte[] result = new byte[GCM_IV_LENGTH + cipherText.length];
        System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
        System.arraycopy(cipherText, 0, result, GCM_IV_LENGTH, cipherText.length);
        return result;
    }

    /**
     * 事前導出済みの鍵で [12B IV + 暗号文 + 16B タグ] を AES-GCM 復号し、平文バイト列を返します。
     */
    public static byte[] decrypt(byte[] cipherMessage, SecretKey secretKey) throws GeneralSecurityException {
        Objects.requireNonNull(secretKey, "secretKey must not be null");
        if (cipherMessage == null || cipherMessage.length < MIN_CIPHER_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Invalid cipher message length");
        }

        // オフセット指定により IV・暗号文の中間配列コピーを回避する
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, cipherMessage, 0, GCM_IV_LENGTH);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        return cipher.doFinal(cipherMessage, GCM_IV_LENGTH, cipherMessage.length - GCM_IV_LENGTH);
    }
}
