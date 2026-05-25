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
 * AI 辅助 REST 控制器。
 * <p>
 * 提供自然语言生成 Transform 节点、向量检索相似历史指令，以及用户对生成结果的反馈闭环。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI辅助", description = "AI辅助相关接口")
public class AIController {

    private final AIService aiService;
    private final UserService userService;

    /**
     * 根据自然语言指令生成转换节点（LLM 或历史模式），并写入 ai_helpers。
     *
     * @param request 指令、可选 schema 上下文与生成选项
     * @return 节点列表、来源信息、aiHelperId 等
     */
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

    /**
     * 基于 embedding 检索与当前指令相似的历史记录。
     *
     * @param request 查询文本、条数上限与相似度阈值
     * @return 相似结果列表
     */
    @PostMapping("/search-similar")
    @Operation(summary = "搜索相似指令")
    public ApiResponse<SearchSimilarResponse> searchSimilar(@Valid @RequestBody SearchSimilarRequest request) {
        log.info("Search similar request: {}", request.getInstruction());
        SearchSimilarResponse response = aiService.searchSimilar(request);
        return ApiResponse.ofSuccess(response);
    }

    /**
     * 提交用户对 AI 生成结果的反馈（accept / modify / reject）。
     *
     * @param request aiHelperId、action 及可选修改后的节点
     * @return 空 data 的成功响应
     */
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
