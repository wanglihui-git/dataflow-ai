package com.dataflow.ai.infrastructure.security;

import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

    public static void main(String[] args) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        System.out.println(bCryptPasswordEncoder.encode("111111"));
        System.out.println(bCryptPasswordEncoder.matches("111111", "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH"));

        User build = User.builder()
                .id("1")
                .username("admin")
                .email("admin@example.com")
                .passwordHash("111111")
                .role(UserRole.ADMIN)
                .department("IT")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
        System.out.println(build);
    }

}
