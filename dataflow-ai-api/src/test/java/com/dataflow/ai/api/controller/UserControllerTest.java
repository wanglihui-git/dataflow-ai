package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.TestSecurityConfig;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import com.dataflow.ai.api.support.WithMockUserId;
import com.dataflow.ai.domain.request.ChangePasswordRequest;
import com.dataflow.ai.domain.request.CreateUserRequest;
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
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Assertions;

/**
 * UserController 的 WebMvc 测试，含 ADMIN 角色与改密接口。
 */

@WebMvcTest
@Import({UserController.class, TestSecurityConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    /**
     * 每个用例执行前初始化 Mock 与测试数据。
     */
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-001")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .role(UserRole.DEVELOPER)
                .department("技术部")
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 验证：GET /v1/users - 管理员成功。
     */
    @Test
    @DisplayName("GET /v1/users - 管理员成功")
    @WithMockUser(roles = "ADMIN")
    void list_withAdmin_returnsUsers() throws Exception {
        // 准备：配置 Mock 返回值
        when(userService.findAllUsers()).thenReturn(List.of(testUser));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/users").accept(MediaType.APPLICATION_JSON))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                // 断言：校验响应或交互
                .andExpect(jsonPath("$.data[0].username").value("testuser"));

        verify(userService).findAllUsers();
    }

    /**
     * 验证：GET /v1/users - 非管理员 403。
     */
    @Test
    @DisplayName("GET /v1/users - 非管理员 403")
    @WithMockUser(roles = "DEVELOPER")
    void list_withoutAdmin_forbidden() throws Exception {
        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/users").accept(MediaType.APPLICATION_JSON))
                // 断言：校验响应或交互
                .andExpect(status().isForbidden());
    }

    /**
     * 验证：GET /v1/users/{id} - 成功。
     */
    @Test
    @DisplayName("GET /v1/users/{id} - 成功")
    @WithMockUser
    void get_existingUser_returnsUser() throws Exception {
        // 准备：配置 Mock 返回值
        when(userService.findById("user-001")).thenReturn(Optional.of(testUser));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/users/user-001").accept(MediaType.APPLICATION_JSON))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("user-001"));
    }

    /**
     * 验证：GET /v1/users/{id} - 不存在时抛出 RuntimeException（GlobalExceptionHandler 未实现）。
     */
    @Test
    @DisplayName("GET /v1/users/{id} - 不存在时抛出 RuntimeException（GlobalExceptionHandler 未实现）")
    @WithMockUser
    void get_missingUser_throwsRuntimeException() {
        // 准备：配置 Mock 返回值
        when(userService.findById("missing")).thenReturn(Optional.empty());

        // 断言：校验响应或交互
        ServletException ex = Assertions.assertThrows(ServletException.class, () ->
                // 执行：发起 HTTP 请求
                mockMvc.perform(get("/v1/users/missing").accept(MediaType.APPLICATION_JSON)));
        assertInstanceOf(RuntimeException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("用户不存在"));
    }

    /**
     * 验证：POST /v1/users - 管理员创建。
     */
    @Test
    @DisplayName("POST /v1/users - 管理员创建")
    @WithMockUser(roles = "ADMIN")
    void create_withAdmin_returnsUser() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .role(UserRole.ANALYST)
                .department("分析部")
                .build();
        // 准备：配置 Mock 返回值
        when(userService.createUser(
                eq("newuser"), eq("new@example.com"), eq("password123"),
                eq(UserRole.ANALYST), eq("分析部"))).thenReturn(testUser);

        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 验证：PUT /v1/users/{id} - 管理员更新。
     */
    @Test
    @DisplayName("PUT /v1/users/{id} - 管理员更新")
    @WithMockUser(roles = "ADMIN")
    void update_withAdmin_returnsUser() throws Exception {
        // 准备：配置 Mock 返回值
        when(userService.updateUser(eq("user-001"), any())).thenReturn(testUser);

        // 执行：发起 HTTP 请求（仅更新 department，验证部分更新请求体）
        mockMvc.perform(put("/v1/users/user-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department\":\"Platform\"}"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("user-001"));
    }

    /**
     * 验证：PUT /v1/users/me/password - 修改当前用户密码。
     */
    @Test
    @DisplayName("PUT /v1/users/me/password - 修改当前用户密码")
    @WithMockUserId("user-001")
    void changeMyPassword_success() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("old-pass")
                .newPassword("new-pass-123")
                .build();
        // 准备：配置 Mock 返回值
        doNothing().when(userService).changePassword("user-001", "old-pass", "new-pass-123");

        // 执行：发起 HTTP 请求
        mockMvc.perform(put("/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 断言：校验响应或交互
        verify(userService).changePassword("user-001", "old-pass", "new-pass-123");
    }

    /**
     * 验证：DELETE /v1/users/{id} - 管理员删除。
     */
    @Test
    @DisplayName("DELETE /v1/users/{id} - 管理员删除")
    @WithMockUser(roles = "ADMIN")
    void delete_withAdmin_success() throws Exception {
        // 准备：配置 Mock 返回值
        doNothing().when(userService).deleteUser("user-001");

        // 执行：发起 HTTP 请求
        mockMvc.perform(delete("/v1/users/user-001"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 断言：校验响应或交互
        verify(userService).deleteUser("user-001");
    }
}
