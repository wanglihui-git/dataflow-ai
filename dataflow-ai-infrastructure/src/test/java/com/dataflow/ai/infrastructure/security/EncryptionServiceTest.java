package com.dataflow.ai.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncryptionServiceTest {

    private static final String KEY_32 = "01234567890123456789012345678901";

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", KEY_32);
        ReflectionTestUtils.invokeMethod(encryptionService, "validateKey");
    }

    @Test
    @DisplayName("resolveKeyBytes - 32 字节密钥通过")
    void resolveKeyBytes_validKey() {
        byte[] bytes = EncryptionService.resolveKeyBytes(KEY_32);
        assertEquals(32, bytes.length);
    }

    @Test
    @DisplayName("resolveKeyBytes - 非 32 字节拒绝")
    void resolveKeyBytes_invalidLength() {
        assertThrows(IllegalStateException.class,
                () -> EncryptionService.resolveKeyBytes("too-short"));
    }

    @Test
    @DisplayName("encrypt/decrypt - 字符串往返")
    void encryptDecrypt_stringRoundTrip() {
        String plain = "jdbc:mysql://localhost:3306/secret";
        String cipher = encryptionService.encrypt(plain);
        assertNotEquals(plain, cipher);
        assertEquals(plain, encryptionService.decrypt(cipher));
    }

    @Test
    @DisplayName("encrypt/decrypt - Map 仅加密字符串字段")
    void encryptDecrypt_mapRoundTrip() {
        Map<String, Object> plain = Map.of("url", "http://x", "port", 3306);
        Map<String, Object> encrypted = encryptionService.encrypt(plain);
        assertNotEquals(plain.get("url"), encrypted.get("url"));
        assertEquals(3306, encrypted.get("port"));

        Map<String, Object> decrypted = encryptionService.decrypt(encrypted);
        assertEquals("http://x", decrypted.get("url"));
        assertEquals(3306, decrypted.get("port"));
    }
}
