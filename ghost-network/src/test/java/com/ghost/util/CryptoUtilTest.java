package com.ghost.util;

import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CryptoUtil} の AES-GCM 暗号化・復号および異常系ハンドリングを検証するユニットテスト。
 */
class CryptoUtilTest {

    @Test
    void encryptAndDecrypt_WithCachedKey_Succeeds() throws Exception {
        SecretKey key = CryptoUtil.deriveKey("super-secret-password");
        byte[] plain = "Hello, Ghost Mod Binary Protocol!".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted1 = CryptoUtil.encrypt(plain, key);
        byte[] encrypted2 = CryptoUtil.encrypt(plain, key);

        // ランダム IV により同一平文でも毎回暗号文が異なること
        assertFalse(java.util.Arrays.equals(encrypted1, encrypted2));

        assertArrayEquals(plain, CryptoUtil.decrypt(encrypted1, key));
        assertArrayEquals(plain, CryptoUtil.decrypt(encrypted2, key));
    }

    @Test
    void encryptAndDecrypt_EmptyPayload_Succeeds() throws Exception {
        SecretKey key = CryptoUtil.deriveKey("pass");
        byte[] encrypted = CryptoUtil.encrypt(new byte[0], key);

        // 最小サイズ（12B IV + 16B GCM タグ = 28B）
        assertEquals(28, encrypted.length);
        assertArrayEquals(new byte[0], CryptoUtil.decrypt(encrypted, key));
    }

    @Test
    void decrypt_TooShortOrNull_ThrowsIllegalArgumentException() {
        SecretKey key = CryptoUtil.deriveKey("pass");

        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.decrypt(null, key));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.decrypt(new byte[0], key));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.decrypt(new byte[27], key));
    }

    @Test
    void decrypt_TamperedCiphertext_ThrowsAEADBadTagException() throws Exception {
        SecretKey key = CryptoUtil.deriveKey("pass");
        byte[] encrypted = CryptoUtil.encrypt("important-payload".getBytes(StandardCharsets.UTF_8), key);

        // 末尾 1 バイトを改ざん
        encrypted[encrypted.length - 1] ^= 0x01;

        assertThrows(AEADBadTagException.class, () -> CryptoUtil.decrypt(encrypted, key));
    }

    @Test
    void decrypt_WithWrongKey_ThrowsAEADBadTagException() throws Exception {
        SecretKey key1 = CryptoUtil.deriveKey("password-1");
        SecretKey key2 = CryptoUtil.deriveKey("password-2");
        byte[] encrypted = CryptoUtil.encrypt("secret".getBytes(StandardCharsets.UTF_8), key1);

        assertThrows(AEADBadTagException.class, () -> CryptoUtil.decrypt(encrypted, key2));
    }
}
