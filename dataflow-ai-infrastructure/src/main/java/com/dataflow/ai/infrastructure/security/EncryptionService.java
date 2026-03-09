package com.dataflow.ai.infrastructure.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 加密服务
 */
@Component
public class EncryptionService {

    @Value("${encryption.secret:defaultEncryptionKey16bytes}")
    private String secret;

    private AES getAES() {
        // 确保密钥长度为16位
        String key = secret.length() < 16 ? String.format("%-16s", secret) : secret.substring(0, 16);
        return SecureUtil.aes(key.getBytes());
    }

    /**
     * 加密字符串
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        AES aes = getAES();
        byte[] encrypted = aes.encrypt(plaintext);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 解密字符串
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            AES aes = getAES();
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            byte[] decrypted = aes.decrypt(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 加密Map配置
     */
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

    /**
     * 解密Map配置
     */
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
                    // 解密失败，保持原值
                    decrypted.put(entry.getKey(), entry.getValue());
                }
            } else {
                decrypted.put(entry.getKey(), entry.getValue());
            }
        }
        return decrypted;
    }
}
