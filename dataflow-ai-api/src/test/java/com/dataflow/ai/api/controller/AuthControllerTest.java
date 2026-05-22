package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.TestSecurityConfig;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.request.LoginRequest;
import com.dataflow.ai.domain.request.RefreshTokenRequest;
import com.dataflow.ai.domain.response.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 的 @WebMvcTest 切片测试，Mock UserService。
 */

@WebMvcTest
@Import({AuthController.class, TestSecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 验证：POST /v1/auth/login - 成功。
     */
    @Test
    @DisplayName("POST /v1/auth/login - 成功")
    void login_success() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("jwt-token")
                .userId("user-001")
                .username("admin")
                .role("ADMIN")
                .build();
        // 准备：配置 Mock 返回值
        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = LoginRequest.builder()
                .username("admin")
                .password("secret")
                .build();

        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                // 断言：校验响应或交互
                .andExpect(jsonPath("$.data.userId").value("user-001"));

        verify(userService).login(any(LoginRequest.class));
    }

    /**
     * 验证：POST /v1/auth/login - 参数为空返回 400。
     */
    @Test
    @DisplayName("POST /v1/auth/login - 参数为空返回 400")
    void login_validationFails() throws Exception {
        LoginRequest request = LoginRequest.builder().username("").password("").build();

        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 断言：校验响应或交互
                .andExpect(status().isBadRequest());
    }

    /**
     * 验证：POST /v1/auth/refresh - 成功。
     */
    @Test
    @DisplayName("POST /v1/auth/refresh - 成功")
    void refresh_success() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("new-access-token")
                .refreshToken("new-refresh-token")
                .userId("user-001")
                .username("admin")
                .role("ADMIN")
                .build();
        // 准备：配置 Mock 返回值
        when(userService.refreshToken("old-refresh")).thenReturn(response);

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("old-refresh")
                .build();

        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("new-access-token"))
                // 断言：校验响应或交互
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));

        verify(userService).refreshToken("old-refresh");
    }

    /**
     * 验证：POST /v1/auth/refresh - refreshToken 为空返回 400。
     */
    @Test
    @DisplayName("POST /v1/auth/refresh - refreshToken 为空返回 400")
    void refresh_validationFails() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("").build();

        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 断言：校验响应或交互
                .andExpect(status().isBadRequest());
    }

    /**
     * 验证：POST /v1/auth/logout - 成功。
     */
    @Test
    @DisplayName("POST /v1/auth/logout - 成功")
    void logout_success() throws Exception {
        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/auth/logout"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
