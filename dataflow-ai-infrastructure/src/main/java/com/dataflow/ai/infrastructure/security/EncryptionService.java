package com.dataflow.ai.infrastructure.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.dataflow.ai.infrastructure.client.datasource.SensitiveConnectionKeys;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源连接配置的 AES 对称加密服务。
 * <p>密钥来自配置项 {@code app.encryption.key}（环境变量 {@code ENCRYPTION_KEY}），
 * 须为 UTF-8 编码下恰好 32 字节；敏感字段名由 {@link com.dataflow.ai.infrastructure.client.datasource.SensitiveConnectionKeys} 判定。
 */
@Component
public class EncryptionService {

    /** AES-256 所需密钥字节长度 */
    public static final int REQUIRED_KEY_BYTES = 32;

    @Value("${app.encryption.key:}")
    private String encryptionKey;

    private byte[] aesKeyBytes;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    /**
     * 将明文字符串加密为 Base64 密文。
     *
     * @param plaintext 明文，{@code null} 时返回 {@code null}
     * @return Base64 编码的密文
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        byte[] encrypted = getAES().encrypt(plaintext);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 将 Base64 密文解密为明文字符串。
     *
     * @param ciphertext Base64 密文，{@code null} 时返回 {@code null}
     * @return 解密后的 UTF-8 明文
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            // 1. Base64 解码  2. AES 解密  3. 转为 UTF-8 字符串
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            byte[] decrypted = getAES().decrypt(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 对连接配置 Map 中敏感键对应的值进行加密（按字段类型选择策略）。
     *
     * @param config 原始连接配置
     * @return 加密后的新 Map，入参为 {@code null} 时返回 {@code null}
     */
    public Map<String, Object> encrypt(Map<String, Object> config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> encrypted = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            encrypted.put(entry.getKey(), encryptValue(entry.getKey(), entry.getValue()));
        }
        return encrypted;
    }

    /**
     * 对连接配置 Map 中已加密的值进行解密（兼容嵌套 JSON 密文）。
     *
     * @param config 可能含密文的连接配置
     * @return 解密后的新 Map，入参为 {@code null} 时返回 {@code null}
     */
    public Map<String, Object> decrypt(Map<String, Object> config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> decrypted = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            decrypted.put(entry.getKey(), decryptValue(entry.getKey(), entry.getValue()));
        }
        return decrypted;
    }

    private Object encryptValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            // 敏感键：字符串直接 AES；非敏感键：原样返回
            return SensitiveConnectionKeys.isSensitive(key) ? encrypt(s) : s;
        }
        if (value instanceof Map || value instanceof Iterable) {
            try {
                // 嵌套结构序列化为 JSON 后整体加密
                String json = objectMapper.writeValueAsString(value);
                return encrypt(json);
            } catch (Exception e) {
                return value;
            }
        }
        if (SensitiveConnectionKeys.isSensitive(key)) {
            return encrypt(String.valueOf(value));
        }
        return value;
    }

    private Object decryptValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            // 非敏感且不像密文：视为明文直接返回
            if (!SensitiveConnectionKeys.isSensitive(key) && !looksLikeBase64Cipher(s)) {
                return s;
            }
            try {
                String plain = decrypt(s);
                // 解密结果为 JSON 对象/数组时反序列化回 Map/List
                if (plain.startsWith("{") || plain.startsWith("[")) {
                    return objectMapper.readValue(plain, new TypeReference<Object>() {});
                }
                return plain;
            } catch (Exception e) {
                return s;
            }
        }
        return value;
    }

    private boolean looksLikeBase64Cipher(String s) {
        return s.length() > 16 && s.matches("^[A-Za-z0-9+/=]+$");
    }
}
