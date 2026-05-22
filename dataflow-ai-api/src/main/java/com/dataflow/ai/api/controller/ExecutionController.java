package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.ResourceAuthorizationHelper;
import com.dataflow.ai.business.service.ExecutionService;
import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.business.service.PipelineService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.business.util.ExecutionLogAppender;
import com.dataflow.ai.common.utils.SecurityUtils;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import com.dataflow.ai.domain.response.ApiResponse;
import com.dataflow.ai.domain.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Pipeline 执行 REST 控制器。
 * <p>
 * 提供执行记录分页查询、详情、日志条目、取消运行及按 Pipeline 聚合统计。
 * 访问执行记录前会校验用户对所属 Pipeline 的权限。
 * </p>
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

    /**
     * 按状态分页查询执行记录。
     * <p>未传 {@code status} 时默认筛选 {@link ExecutionStatus#RUNNING}。</p>
     *
     * @param status 执行状态过滤（可选）
     * @param page   页码
     * @param size   每页大小
     * @return 分页的 {@link ExecutionRun} 列表
     */
    @GetMapping("/runs")
    @Operation(summary = "分页查询执行记录")
    public ApiResponse<PageResponse<ExecutionRun>> listRuns(
            @RequestParam(required = false) ExecutionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ExecutionStatus filter = status != null ? status : ExecutionStatus.RUNNING;
        Page<ExecutionRun> result = executionService.findByStatus(filter, PageRequest.of(page, size));
        return ApiResponse.ofSuccess(PageResponse.of(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    /**
     * 查询单次执行详情。
     *
     * @param runId 执行记录 ID
     * @return {@link ExecutionRun} 含状态、指标与日志 JSON
     */
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

    /**
     * 查询执行过程日志条目（从 executionLog.entries 解析）。
     *
     * @param runId 执行记录 ID
     * @return 日志条目列表，每项含 timestamp、phase、message
     */
    @GetMapping("/runs/{runId}/logs")
    @Operation(summary = "查询执行日志")
    public ApiResponse<List<Map<String, Object>>> getRunLogs(@PathVariable String runId) {
        ExecutionRun run = executionService.findById(runId)
                .orElseThrow(() -> new RuntimeException("执行记录不存在"));
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(run.getPipelineId())
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineAccess(pipeline, user, permissionService);
        return ApiResponse.ofSuccess(ExecutionLogAppender.getEntries(run));
    }

    /**
     * 取消正在运行或待处理的执行。
     *
     * @param runId 执行记录 ID
     * @return 空 data 的成功响应
     */
    @PostMapping("/runs/{runId}/cancel")
    @Operation(summary = "取消执行")
    public ApiResponse<Void> cancel(@PathVariable String runId) {
        ExecutionRun run = executionService.findById(runId)
                .orElseThrow(() -> new RuntimeException("执行记录不存在"));
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(run.getPipelineId())
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        // 取消需要执行权（与触发 run 同级）
        ResourceAuthorizationHelper.requirePipelineExecute(pipeline, user, permissionService);
        executionService.cancelExecution(runId);
        return ApiResponse.ofSuccess();
    }

    /**
     * 统计指定 Pipeline 的执行次数与成功率。
     *
     * @param pipelineId Pipeline ID
     * @return total、success、failed、successRate
     */
    @GetMapping("/pipelines/{pipelineId}/stats")
    @Operation(summary = "获取Pipeline执行统计")
    public ApiResponse<Map<String, Object>> getStats(@PathVariable String pipelineId) {
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(pipelineId)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineAccess(pipeline, user, permissionService);
        return ApiResponse.ofSuccess(executionService.getExecutionStats(pipelineId));
    }

    /**
     * 获取当前登录用户。
     *
     * @return 用户实体
     */
    private User requireCurrentUser() {
        return userService.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
