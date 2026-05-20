package com.dataflow.ai.infrastructure.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源连接配置 AES 加密服务（密钥来自 app.encryption.key，须 32 字节）
 */
@Component
public class EncryptionService {

    public static final int REQUIRED_KEY_BYTES = 32;

    @Value("${app.encryption.key:}")
    private String encryptionKey;

    private byte[] aesKeyBytes;

    @PostConstruct
    void validateKey() {
        aesKeyBytes = resolveKeyBytes(encryptionKey);
    }

    static byte[] resolveKeyBytes(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "app.encryption.key (ENCRYPTION_KEY) must be set and exactly " + REQUIRED_KEY_BYTES + " bytes");
        }
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        if (raw.length != REQUIRED_KEY_BYTES) {
            throw new IllegalStateException(
                    "app.encryption.key must be exactly " + REQUIRED_KEY_BYTES + " bytes, got " + raw.length);
        }
        return raw;
    }

    private AES getAES() {
        return SecureUtil.aes(aesKeyBytes);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        byte[] encrypted = getAES().encrypt(plaintext);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            byte[] decrypted = getAES().decrypt(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    public Map<String, Object> encrypt(Map<String, Object> config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> encrypted = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof String) {
                encrypted.put(entry.getKey(), encrypt((String) entry.getValue()));
            } else {
                encrypted.put(entry.getKey(), entry.getValue());
            }
        }
        return encrypted;
    }

    public Map<String, Object> decrypt(Map<String, Object> config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> decrypted = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof String) {
                try {
                    decrypted.put(entry.getKey(), decrypt((String) entry.getValue()));
                } catch (Exception e) {
                    decrypted.put(entry.getKey(), entry.getValue());
                }
            } else {
                decrypted.put(entry.getKey(), entry.getValue());
            }
        }
        return decrypted;
    }
}
