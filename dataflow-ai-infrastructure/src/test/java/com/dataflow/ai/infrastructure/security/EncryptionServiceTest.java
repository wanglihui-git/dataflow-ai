package com.dataflow.ai.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EncryptionService 加解密与 Map 敏感字段单测。
 */

class EncryptionServiceTest {

    private static final String KEY_32 = "01234567890123456789012345678901";

    private EncryptionService encryptionService;

    /**
     * 每个用例执行前初始化 Mock 与测试数据。
     */
    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", KEY_32);
        ReflectionTestUtils.invokeMethod(encryptionService, "validateKey");
    }

    /**
     * 验证：resolveKeyBytes - 32 字节密钥通过。
     */
    @Test
    @DisplayName("resolveKeyBytes - 32 字节密钥通过")
    void resolveKeyBytes_validKey() {
        byte[] bytes = EncryptionService.resolveKeyBytes(KEY_32);
        // 断言：校验响应或交互
        assertEquals(32, bytes.length);
    }

    /**
     * 验证：resolveKeyBytes - 非 32 字节拒绝。
     */
    @Test
    @DisplayName("resolveKeyBytes - 非 32 字节拒绝")
    void resolveKeyBytes_invalidLength() {
        // 断言：校验响应或交互
        assertThrows(IllegalStateException.class,
                () -> EncryptionService.resolveKeyBytes("too-short"));
    }

    /**
     * 验证：encrypt/decrypt - 字符串往返。
     */
    @Test
    @DisplayName("encrypt/decrypt - 字符串往返")
    void encryptDecrypt_stringRoundTrip() {
        String plain = "jdbc:mysql://localhost:3306/secret";
        // 执行：调用被测方法
        String cipher = encryptionService.encrypt(plain);
        assertNotEquals(plain, cipher);
        // 断言：校验响应或交互
        assertEquals(plain, encryptionService.decrypt(cipher));
    }

    /**
     * 验证：encrypt/decrypt - Map 仅加密字符串字段。
     */
    @Test
    @DisplayName("encrypt/decrypt - Map 仅加密字符串字段")
    void encryptDecrypt_mapRoundTrip() {
        Map<String, Object> plain = Map.of("url", "http://x", "port", 3306);
        // 执行：调用被测方法
        Map<String, Object> encrypted = encryptionService.encrypt(plain);
        assertNotEquals(plain.get("url"), encrypted.get("url"));
        // 断言：校验响应或交互
        assertEquals(3306, encrypted.get("port"));

        Map<String, Object> decrypted = encryptionService.decrypt(encrypted);
        // 断言：校验响应或交互
        assertEquals("http://x", decrypted.get("url"));
        assertEquals(3306, decrypted.get("port"));
    }

    /**
     * 验证：encrypt/decrypt - 敏感非字符串字段加密。
     */
    @Test
    @DisplayName("encrypt/decrypt - 敏感非字符串字段加密")
    void encryptDecrypt_sensitiveNonString() {
        Map<String, Object> plain = Map.of("password", 12345);
        // 执行：调用被测方法
        Map<String, Object> encrypted = encryptionService.encrypt(plain);
        assertTrue(encrypted.get("password") instanceof String);
        assertNotEquals(12345, encrypted.get("password"));

        Map<String, Object> decrypted = encryptionService.decrypt(encrypted);
        // 断言：校验响应或交互
        assertEquals("12345", decrypted.get("password"));
    }
}
