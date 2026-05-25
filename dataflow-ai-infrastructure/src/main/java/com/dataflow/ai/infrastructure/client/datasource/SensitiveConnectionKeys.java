package com.dataflow.ai.infrastructure.client.datasource;

import java.util.Set;

/**
 * 数据源连接 JSON 配置中的敏感字段名集合，供 {@link com.dataflow.ai.infrastructure.security.EncryptionService} 判定是否加密。
 */
public final class SensitiveConnectionKeys {

    /** 常见敏感键名（小写匹配） */
    public static final Set<String> SENSITIVE_KEYS = Set.of(
            "url", "username", "password", "secret", "apikey", "api_key", "token",
            "accesskey", "access_key", "privatekey", "private_key", "credential", "credentials"
    );

    private SensitiveConnectionKeys() {
    }

    /**
     * 判断配置键是否应视为敏感（精确匹配或包含 password/secret 子串）。
     *
     * @param key 配置项键名
     * @return 敏感返回 {@code true}
     */
    public static boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        return SENSITIVE_KEYS.contains(lower) || lower.contains("password") || lower.contains("secret");
    }
}
