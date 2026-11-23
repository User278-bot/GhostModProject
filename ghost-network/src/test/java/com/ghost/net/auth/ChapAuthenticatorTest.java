package com.ghost.net.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ChapAuthenticator} の HMAC-SHA256 チャレンジ・レスポンス認証を検証するユニットテスト。
 */
class ChapAuthenticatorTest {

    @Test
    void generateNonce_ReturnsUniqueNonEmptyValues() {
        String n1 = ChapAuthenticator.generateNonce();
        String n2 = ChapAuthenticator.generateNonce();

        assertNotNull(n1);
        assertFalse(n1.isEmpty());
        assertNotEquals(n1, n2);
    }

    @Test
    void verify_MatchingResponse_ReturnsTrue() throws Exception {
        String password = "my-strong-password";
        String nonce = ChapAuthenticator.generateNonce();
        String hash = ChapAuthenticator.calculateHash(password, nonce);

        assertTrue(ChapAuthenticator.verify(password, nonce, hash));
    }

    @Test
    void verify_MismatchedPasswordNonceOrHash_ReturnsFalse() throws Exception {
        String nonce = ChapAuthenticator.generateNonce();
        String validHash = ChapAuthenticator.calculateHash("correct-password", nonce);

        assertFalse(ChapAuthenticator.verify("wrong-password", nonce, validHash));
        assertFalse(ChapAuthenticator.verify("correct-password", nonce + "x", validHash));
        assertFalse(ChapAuthenticator.verify("correct-password", nonce, validHash + "tampered"));
    }

    @Test
    void verify_NullArguments_ReturnsFalse() {
        assertFalse(ChapAuthenticator.verify(null, "nonce", "hash"));
        assertFalse(ChapAuthenticator.verify("pass", null, "hash"));
        assertFalse(ChapAuthenticator.verify("pass", "nonce", null));
    }
}
