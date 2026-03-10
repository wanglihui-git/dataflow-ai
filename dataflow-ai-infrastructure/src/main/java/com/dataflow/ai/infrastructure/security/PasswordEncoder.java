package com.dataflow.ai.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码编码器
 */
@Component
public class PasswordEncoder implements org.springframework.security.crypto.password.PasswordEncoder{

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 编码密码
     */
    @Override
    public String encode(CharSequence rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 检查密码是否匹配
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
