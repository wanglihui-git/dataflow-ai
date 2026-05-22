package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.UserRepository;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import com.dataflow.ai.domain.exception.BusinessException;
import com.dataflow.ai.domain.request.LoginRequest;
import com.dataflow.ai.domain.request.UpdateUserRequest;
import com.dataflow.ai.domain.response.LoginResponse;
import com.dataflow.ai.infrastructure.security.JwtProvider;
import com.dataflow.ai.infrastructure.security.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserServiceImpl 登录、刷新、改密与用户 CRUD 单测。
 */

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private UserServiceImpl userService;

    /**
     * 测试方法 injectFieldDependencies。
     */
    @BeforeEach
    void injectFieldDependencies() {
        // UserServiceImpl 使用 @Resource 注入 userRepository，需手动绑定 Mock
        ReflectionTestUtils.setField(userService, "userRepository", userRepository);
    }

    /**
     * 验证：login - 成功返回 token。
     */
    @Test
    @DisplayName("login - 成功返回 token")
    void login_success() {
        User user = User.builder()
                .id("user-001")
                .username("admin")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .department("IT")
                .build();
        // 准备：配置 Mock 返回值
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtProvider.generateAccessToken("user-001", "admin", "ADMIN")).thenReturn("jwt");
        when(jwtProvider.generateRefreshToken("user-001", "admin", "ADMIN")).thenReturn("refresh");

        // 执行：调用被测方法
        LoginResponse response = userService.login(
                LoginRequest.builder().username("admin").password("secret").build());

        // 断言：校验响应或交互
        assertEquals("jwt", response.getToken());
        assertEquals("user-001", response.getUserId());
        verify(userRepository).updateLastLoginAt("user-001");
    }

    /**
     * 验证：login - 用户不存在。
     */
    @Test
    @DisplayName("login - 用户不存在")
    void login_userNotFound() {
        // 准备：配置 Mock 返回值
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        // 断言：校验响应或交互
        assertThrows(BusinessException.class, () -> userService.login(
                LoginRequest.builder().username("nobody").password("x").build()));
    }

    /**
     * 验证：createUser - 持久化。
     */
    @Test
    @DisplayName("createUser - 持久化")
    void createUser_savesUser() {
        // 准备：配置 Mock 返回值
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.createUser(
                "u1", "u1@test.com", "pwd", UserRole.DEVELOPER, "dept");

        // 断言：校验响应或交互
        assertNotNull(created.getId());
        verify(userRepository).save(any(User.class));
    }

    /**
     * 验证：findById - 委托 Repository。
     */
    @Test
    @DisplayName("findById - 委托 Repository")
    void findById_delegatesToRepository() {
        // 准备：配置 Mock 返回值
        when(userRepository.findById("user-001")).thenReturn(Optional.empty());

        // 执行：调用被测方法
        userService.findById("user-001");

        // 断言：校验响应或交互
        verify(userRepository).findById("user-001");
    }

    /**
     * 验证：updateUser - 用户不存在时返回 404 业务异常。
     */
    @Test
    @DisplayName("updateUser - 用户不存在")
    void updateUser_missingUser_throwsNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateUser("missing", UpdateUserRequest.builder().department("IT").build()));

        assertEquals(404, ex.getCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    /**
     * 验证：updateUser - 仅合并非 null 字段，保留 username 等必填列。
     */
    @Test
    @DisplayName("updateUser - 部分字段更新")
    void updateUser_partialFields_mergesIntoExisting() {
        User existing = User.builder()
                .id("user-001")
                .username("admin")
                .email("admin@test.com")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .department("IT")
                .status("active")
                .build();
        when(userRepository.findById("user-001")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.updateUser("user-001",
                UpdateUserRequest.builder().department("Platform").build());

        assertEquals("admin", updated.getUsername());
        assertEquals("admin@test.com", updated.getEmail());
        assertEquals("hash", updated.getPasswordHash());
        assertEquals(UserRole.ADMIN, updated.getRole());
        assertEquals("Platform", updated.getDepartment());
        verify(userRepository).save(existing);
    }
}
