package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.domain.exception.BusinessException;
import com.dataflow.ai.domain.request.LoginRequest;
import com.dataflow.ai.domain.response.LoginResponse;
import com.dataflow.ai.domain.response.ResponseCode;
import com.dataflow.ai.business.repository.UserRepository;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import com.dataflow.ai.infrastructure.security.JwtProvider;
import com.dataflow.ai.infrastructure.security.PasswordEncoder;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link UserService} 实现：JWT 签发、密码校验与用户持久化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Resource
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /** {@inheritDoc} */
    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ResponseCode.CODE_401, "用户名或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResponseCode.CODE_401, "用户名或密码错误");
        }
        updateLastLogin(user.getId());
        return buildLoginResponse(user);
    }

    /** {@inheritDoc} */
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ResponseCode.CODE_401, "刷新令牌无效或已过期");
        }
        try {
            String access = jwtProvider.refreshAccessToken(refreshToken);
            String userId = jwtProvider.getUserIdFromToken(refreshToken);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ResponseCode.CODE_401, "用户不存在"));
            return LoginResponse.builder()
                    .token(access)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .department(user.getDepartment())
                    .build();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResponseCode.CODE_401, e.getMessage());
        }
    }

    /**
     * 为用户生成 access/refresh 令牌并组装登录响应。
     *
     * @param user 已认证用户
     * @return 登录响应 DTO
     */
    private LoginResponse buildLoginResponse(User user) {
        return LoginResponse.builder()
                .token(jwtProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name()))
                .refreshToken(jwtProvider.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name()))
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .department(user.getDepartment())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /** {@inheritDoc} */
    @Override
    public User createUser(String username, String email, String passwordHash, UserRole role, String department) {
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .role(role)
                .department(department)
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    /** {@inheritDoc} */
    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    /** {@inheritDoc} */
    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /** {@inheritDoc} */
    @Override
    public void updateLastLogin(String userId) {
        userRepository.updateLastLoginAt(userId);
    }

    /** {@inheritDoc} */
    @Override
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CODE_404, "用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(ResponseCode.CODE_400, "原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
