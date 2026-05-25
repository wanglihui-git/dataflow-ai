package com.dataflow.ai.infrastructure.security;

import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 密码编码器：基于 BCrypt 对用户密码进行单向哈希与校验。
 * <p>实现 Spring Security {@link org.springframework.security.crypto.password.PasswordEncoder} 接口，供注册、登录流程注入使用。
 */
@Component
public class PasswordEncoder implements org.springframework.security.crypto.password.PasswordEncoder{

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 将明文密码编码为 BCrypt 哈希串。
     *
     * @param rawPassword 明文密码
     * @return 哈希后的密文
     */
    @Override
    public String encode(CharSequence rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 校验明文密码是否与已存储的 BCrypt 哈希匹配。
     *
     * @param rawPassword      用户输入的明文
     * @param encodedPassword  库中存储的哈希
     * @return 匹配返回 {@code true}
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    /** 本地调试入口：生成示例 BCrypt 哈希 */
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
