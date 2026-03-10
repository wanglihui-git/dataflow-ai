package com.dataflow.ai.api.controller;

import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.common.dto.ApiResponse;
import com.dataflow.ai.config.SecurityConfig;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import com.dataflow.ai.infrastructure.security.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 测试类
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-001")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .role(UserRole.DEVELOPER)
                .department("技术部")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();

        adminUser = User.builder()
                .id("user-002")
                .username("admin")
                .email("admin@example.com")
                .passwordHash("adminpassword")
                .role(UserRole.ADMIN)
                .department("管理部")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("查询用户列表 - 管理员权限成功")
    @WithMockUser(roles = "ADMIN")
    void list_WithAdminRole_ShouldReturnUserList() throws Exception {
        // Given
        List<User> users = Arrays.asList(testUser, adminUser);
        when(userService.findAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].username").value("testuser"))
                .andExpect(jsonPath("$.data[1].username").value("admin"));

        verify(userService, times(1)).findAllUsers();
    }

    @Test
    @DisplayName("查询用户列表 - 无权限访问被拒绝")
    @WithMockUser(roles = "DEVELOPER")
    void list_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("查询用户详情 - 成功")
    @WithMockUser
    void get_WithExistingId_ShouldReturnUser() throws Exception {
        // Given
        when(userService.findById("user-001")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/api/v1/users/user-001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("user-001"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.role").value("DEVELOPER"));

        verify(userService, times(1)).findById("user-001");
    }

    @Test
    @DisplayName("查询用户详情 - 用户不存在抛出异常")
    @WithMockUser
    void get_WithNonExistingId_ShouldThrowException() throws Exception {
        // Given
        when(userService.findById("non-existing-id")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/users/non-existing-id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));

        verify(userService, times(1)).findById("non-existing-id");
    }

    @Test
    @DisplayName("创建用户 - 管理员权限成功")
    @WithMockUser(roles = "ADMIN")
    void create_WithAdminRole_ShouldReturnCreatedUser() throws Exception {
        // Given
        when(userService.createUser(
                eq("newuser"),
                eq("new@example.com"),
                eq("password123"),
                eq(UserRole.ANALYST),
                eq("分析部")
        )).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("password", "password123")
                        .param("role", "ANALYST")
                        .param("department", "分析部")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService, times(1)).createUser(
                eq("newuser"),
                eq("new@example.com"),
                eq("password123"),
                eq(UserRole.ANALYST),
                eq("分析部")
        );
    }

    @Test
    @DisplayName("创建用户 - 无权限访问被拒绝")
    @WithMockUser(roles = "DEVELOPER")
    void create_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("password", "password123")
                        .param("role", "ANALYST")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("更新用户 - 管理员权限成功")
    @WithMockUser(roles = "ADMIN")
    void update_WithAdminRole_ShouldReturnUpdatedUser() throws Exception {
        // Given
        User updatedUser = User.builder()
                .id("user-001")
                .username("updateduser")
                .email("updated@example.com")
                .role(UserRole.DEVELOPER)
                .department("更新部门")
                .build();

        when(userService.updateUser(any(User.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/v1/users/user-001")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("user-001"))
                .andExpect(jsonPath("$.data.username").value("updateduser"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"));

        verify(userService, times(1)).updateUser(any(User.class));
    }

    @Test
    @DisplayName("更新用户 - 无权限访问被拒绝")
    @WithMockUser(roles = "ANALYST")
    void update_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        // Given
        User user = new User();
        user.setUsername("test");

        // When & Then
        mockMvc.perform(put("/api/v1/users/user-001")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("删除用户 - 管理员权限成功")
    @WithMockUser(roles = "ADMIN")
    void delete_WithAdminRole_ShouldReturnSuccess() throws Exception {
        // Given
        doNothing().when(userService).deleteUser("user-001");

        // When & Then
        mockMvc.perform(delete("/api/v1/users/user-001")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService, times(1)).deleteUser("user-001");
    }

    @Test
    @DisplayName("删除用户 - 无权限访问被拒绝")
    @WithMockUser(roles = "VIEWER")
    void delete_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/users/user-001")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("创建用户 - 部门参数可选")
    @WithMockUser(roles = "ADMIN")
    void create_WithoutDepartment_ShouldReturnCreatedUser() throws Exception {
        // Given
        when(userService.createUser(
                anyString(),
                anyString(),
                anyString(),
                any(UserRole.class),
                eq(null)
        )).thenReturn(testUser);

        // When & Then - 不传递 department 参数
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("password", "password123")
                        .param("role", "DEVELOPER")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
