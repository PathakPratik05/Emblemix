package com.emblemix.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionUtilTest {

    private final AesEncryptionUtil encryptionUtil = new AesEncryptionUtil(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    );

    @Test
    @DisplayName("Should encrypt and decrypt plain text credentials losslessly")
    void testEncryptionDecryptionRoundtrip() {
        String secretToken = "00D5g000004XXXX!AQEAQMys.SecretOAuthToken12345";
        String cipherText = encryptionUtil.encrypt(secretToken);

        assertNotNull(cipherText);
        assertNotEquals(secretToken, cipherText);

        String decrypted = encryptionUtil.decrypt(cipherText);
        assertEquals(secretToken, decrypted);
    }

    @Test
    @DisplayName("Should produce different ciphertexts for the same plain text due to random IV")
    void testRandomIvProducesDifferentCiphertext() {
        String token = "my-secret-token";
        String cipher1 = encryptionUtil.encrypt(token);
        String cipher2 = encryptionUtil.encrypt(token);

        assertNotEquals(cipher1, cipher2);
        assertEquals(token, encryptionUtil.decrypt(cipher1));
        assertEquals(token, encryptionUtil.decrypt(cipher2));
    }

    @Test
    @DisplayName("Should handle null strings safely")
    void testNullHandling() {
        assertNull(encryptionUtil.encrypt(null));
        assertNull(encryptionUtil.decrypt(null));
    }
}
