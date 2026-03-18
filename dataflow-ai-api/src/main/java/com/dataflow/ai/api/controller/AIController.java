package com.dataflow.ai.api.controller;

import com.dataflow.ai.business.service.AIService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.common.utils.SecurityUtils;
import com.dataflow.ai.domain.request.FeedbackRequest;
import com.dataflow.ai.domain.request.GenerateTransformsRequest;
import com.dataflow.ai.domain.request.SearchSimilarRequest;
import com.dataflow.ai.domain.response.GenerateTransformsResponse;
import com.dataflow.ai.domain.response.SearchSimilarResponse;
import com.dataflow.ai.domain.response.ApiResponse;
import com.dataflow.ai.domain.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI辅助控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI辅助", description = "AI辅助相关接口")
public class AIController {

    private final AIService aiService;
    private final UserService userService;

    @PostMapping("/generate-transforms")
    @Operation(summary = "AI生成转换节点")
    public ApiResponse<GenerateTransformsResponse> generateTransforms(@Valid @RequestBody GenerateTransformsRequest request) {
        log.info("Generate transforms request: {}", request.getInstruction());
        String userId = SecurityUtils.getCurrentUserId();
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        GenerateTransformsResponse response = aiService.generateTransforms(request, user);
        return ApiResponse.ofSuccess(response);
    }

    @PostMapping("/search-similar")
    @Operation(summary = "搜索相似指令")
    public ApiResponse<SearchSimilarResponse> searchSimilar(@Valid @RequestBody SearchSimilarRequest request) {
        log.info("Search similar request: {}", request.getInstruction());
        SearchSimilarResponse response = aiService.searchSimilar(request);
        return ApiResponse.ofSuccess(response);
    }

    @PostMapping("/feedback")
    @Operation(summary = "提交AI反馈")
    public ApiResponse<Void> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        log.info("Submit feedback: {}", request.getAction());
        String userId = SecurityUtils.getCurrentUserId();
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        aiService.submitFeedback(request, user);
        return ApiResponse.ofSuccess();
    }
}
