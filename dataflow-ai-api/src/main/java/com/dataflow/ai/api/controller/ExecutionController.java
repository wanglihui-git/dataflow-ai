package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.ResourceAuthorizationHelper;
import com.dataflow.ai.business.service.ExecutionService;
import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.business.service.PipelineService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.common.utils.SecurityUtils;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 执行控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/execution")
@RequiredArgsConstructor
@Tag(name = "执行", description = "执行管理相关接口")
public class ExecutionController {

    private final ExecutionService executionService;
    private final PipelineService pipelineService;
    private final UserService userService;
    private final PermissionService permissionService;

    @GetMapping("/runs/{runId}")
    @Operation(summary = "查询执行记录详情")
    public ApiResponse<ExecutionRun> getRun(@PathVariable String runId) {
        ExecutionRun run = executionService.findById(runId)
                .orElseThrow(() -> new RuntimeException("执行记录不存在"));
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(run.getPipelineId())
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineAccess(pipeline, user, permissionService);
        return ApiResponse.ofSuccess(run);
    }

    @PostMapping("/runs/{runId}/cancel")
    @Operation(summary = "取消执行")
    public ApiResponse<Void> cancel(@PathVariable String runId) {
        ExecutionRun run = executionService.findById(runId)
                .orElseThrow(() -> new RuntimeException("执行记录不存在"));
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(run.getPipelineId())
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineExecute(pipeline, user, permissionService);
        executionService.cancelExecution(runId);
        return ApiResponse.ofSuccess();
    }

    @GetMapping("/pipelines/{pipelineId}/stats")
    @Operation(summary = "获取Pipeline执行统计")
    public ApiResponse<Map<String, Object>> getStats(@PathVariable String pipelineId) {
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(pipelineId)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineAccess(pipeline, user, permissionService);
        Map<String, Object> stats = executionService.getExecutionStats(pipelineId);
        return ApiResponse.ofSuccess(stats);
    }

    private User requireCurrentUser() {
        return userService.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
