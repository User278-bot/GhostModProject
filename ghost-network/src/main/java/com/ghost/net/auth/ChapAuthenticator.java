package com.ghost.net.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * HMAC-SHA256 を用いた CHAP（チャレンジ・レスポンス）認証ユーティリティ。
 */
public final class ChapAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChapAuthenticator.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int NONCE_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ChapAuthenticator() {
    }

    /**
     * 暗号学的に安全なランダムチャレンジ（nonce）を生成します。
     */
    public static String generateNonce() {
        byte[] nonceBytes = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonceBytes);
        return Base64.getEncoder().encodeToString(nonceBytes);
    }

    /**
     * クライアントから返送された HMAC-SHA256 レスポンスを検証します。
     *
     * @param password       サーバー側の正しいパスワード
     * @param nonce          クライアントへ送信したチャレンジ文字列
     * @param clientResponse クライアントから受け取ったレスポンスハッシュ
     * @return 検証成功時は true
     */
    public static boolean verify(String password, String nonce, String clientResponse) {
        if (password == null || nonce == null || clientResponse == null) {
            return false;
        }
        try {
            String expectedHash = calculateHash(password, nonce);
            // タイミング攻撃を防ぐため定数時間比較を行う
            return MessageDigest.isEqual(
                    expectedHash.getBytes(StandardCharsets.UTF_8),
                    clientResponse.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            LOGGER.error("Failed to verify CHAP response", e);
            return false;
        }
    }

    /**
     * パスワードを鍵として nonce の HMAC-SHA256 を算出し、Base64 文字列で返します。
     */
    public static String calculateHash(String password, String nonce) throws GeneralSecurityException {
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(nonce, "nonce must not be null");

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] hmacBytes = mac.doFinal(nonce.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}
