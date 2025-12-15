package com.ghost.net.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class ChapAuthenticator {
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a random nonce (challenge string).
     */
    public static String generateNonce() {
        byte[] nonceBytes = new byte[16];
        RANDOM.nextBytes(nonceBytes);
        return Base64.getEncoder().encodeToString(nonceBytes);
    }

    /**
     * Verifies the client's response hash.
     * Expected Hash = SHA-256(password + nonce)
     *
     * @param password       The correct server password.
     * @param nonce          The nonce sent to the client.
     * @param clientResponse The hash received from the client.
     * @return true if the response matches the expected hash.
     */
    public static boolean verify(String password, String nonce, String clientResponse) {
        if (password == null || nonce == null || clientResponse == null) {
            return false;
        }
        try {
            String expectedHash = calculateHash(password, nonce);
            // Use constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    expectedHash.getBytes(StandardCharsets.UTF_8),
                    clientResponse.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String calculateHash(String password, String nonce) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String input = password + nonce;
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}
