package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.TestSecurityConfig;
import com.dataflow.ai.api.support.WithMockUserId;
import com.dataflow.ai.business.service.AIService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import com.dataflow.ai.domain.request.FeedbackRequest;
import com.dataflow.ai.domain.request.GenerateTransformsRequest;
import com.dataflow.ai.domain.request.SearchSimilarRequest;
import com.dataflow.ai.domain.response.GenerateTransformsResponse;
import com.dataflow.ai.domain.response.SearchSimilarResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AIController.class)
@Import(TestSecurityConfig.class)
@WithMockUserId("user-001")
class AIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AIService aiService;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id("user-001").username("dev").role(UserRole.DEVELOPER).build();
        when(userService.findById("user-001")).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("POST /v1/ai/generate-transforms - 生成（LLM 层为 Mock 实现）")
    void generateTransforms_success() throws Exception {
        GenerateTransformsResponse response = GenerateTransformsResponse.builder()
                .nodes(List.of())
                .build();
        when(aiService.generateTransforms(any(), any())).thenReturn(response);

        GenerateTransformsRequest request = GenerateTransformsRequest.builder()
                .instruction("将金额字段转为美元")
                .build();

        mockMvc.perform(post("/v1/ai/generate-transforms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aiService).generateTransforms(any(), any());
    }

    @Test
    @DisplayName("POST /v1/ai/search-similar - 相似搜索")
    void searchSimilar_success() throws Exception {
        when(aiService.searchSimilar(any())).thenReturn(
                SearchSimilarResponse.builder().results(List.of()).build());

        SearchSimilarRequest request = SearchSimilarRequest.builder()
                .instruction("过滤空值")
                .build();

        mockMvc.perform(post("/v1/ai/search-similar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /v1/ai/feedback - 反馈")
    void submitFeedback_success() throws Exception {
        FeedbackRequest request = FeedbackRequest.builder()
                .aiHelperId("ai-001")
                .action("accept")
                .build();

        mockMvc.perform(post("/v1/ai/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aiService).submitFeedback(any(), any());
    }
}
