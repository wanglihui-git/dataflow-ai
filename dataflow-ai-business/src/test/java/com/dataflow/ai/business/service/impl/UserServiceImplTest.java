package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.UserRepository;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import com.dataflow.ai.domain.request.LoginRequest;
import com.dataflow.ai.domain.response.LoginResponse;
import com.dataflow.ai.infrastructure.security.JwtProvider;
import com.dataflow.ai.infrastructure.security.PasswordEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtProvider.generateToken("user-001", "admin", "ADMIN")).thenReturn("jwt");

        LoginResponse response = userService.login(
                LoginRequest.builder().username("admin").password("secret").build());

        assertEquals("jwt", response.getToken());
        assertEquals("user-001", response.getUserId());
        verify(userRepository).updateLastLoginAt("user-001");
    }

    @Test
    @DisplayName("login - 用户不存在")
    void login_userNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.login(
                LoginRequest.builder().username("nobody").password("x").build()));
    }

    @Test
    @DisplayName("createUser - 持久化")
    void createUser_savesUser() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.createUser(
                "u1", "u1@test.com", "pwd", UserRole.DEVELOPER, "dept");

        assertNotNull(created.getId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("findById - 委托 Repository")
    void findById_delegatesToRepository() {
        when(userRepository.findById("user-001")).thenReturn(Optional.empty());

        userService.findById("user-001");

        verify(userRepository).findById("user-001");
    }
}
