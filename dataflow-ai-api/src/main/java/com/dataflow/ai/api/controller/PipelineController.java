package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.ResourceAuthorizationHelper;
import com.dataflow.ai.business.service.PipelineService;
import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.common.utils.SecurityUtils;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.request.CreatePipelineRequest;
import com.dataflow.ai.domain.request.UpdatePipelineRequest;
import com.dataflow.ai.domain.response.ApiResponse;
import com.dataflow.ai.domain.response.PageResponse;
import org.springframework.data.domain.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Pipeline REST 控制器。
 * <p>
 * 管理数据处理流水线（Source → Transform → Sink），支持 CRUD、异步执行、
 * 执行历史查询与转换结果预览。资源级权限由 {@link ResourceAuthorizationHelper} 校验。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/v1/pipelines")
@RequiredArgsConstructor
@Tag(name = "Pipeline", description = "Pipeline管理相关接口")
public class PipelineController {

    private final PipelineService pipelineService;
    private final UserService userService;
    private final PermissionService permissionService;

    /**
     * 创建 Pipeline，所有者设为当前用户。
     *
     * @param request 名称、source、transforms、sink、schedule 等
     * @return 持久化后的 Pipeline
     */
    @PostMapping
    @Operation(summary = "创建Pipeline")
    public ApiResponse<Pipeline> create(@Valid @RequestBody CreatePipelineRequest request) {
        log.info("Create pipeline: {}", request.getName());
        String userId = SecurityUtils.getCurrentUserId();
        Pipeline pipeline = pipelineService.createPipeline(request, userId);
        return ApiResponse.ofSuccess(pipeline);
    }

    /**
     * 分页查询当前用户可访问的 Pipeline。
     *
     * @param name 名称过滤（可选）
     * @param page 页码，从 0 开始
     * @param size 每页条数
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "查询Pipeline列表")
    public ApiResponse<PageResponse<Pipeline>> list(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = SecurityUtils.getCurrentUserId();
        PageResponse<Pipeline> pipelines = pipelineService.findByUserPage(
                userId, name, PageRequest.of(page, size));
        return ApiResponse.ofSuccess(pipelines);
    }

    /**
     * 查询 Pipeline 详情。
     *
     * @param id Pipeline ID
     * @return Pipeline 实体（含 JSONB 配置）
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询Pipeline详情")
    public ApiResponse<Pipeline> get(@PathVariable String id) {
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineAccess(pipeline, user, permissionService);
        return ApiResponse.ofSuccess(pipeline);
    }

    /**
     * 更新 Pipeline 配置；请求体仅非 null 字段会写入，未传字段保持原值。
     *
     * @param id      Pipeline ID
     * @param request 待更新字段
     * @return 更新后的 Pipeline
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新Pipeline")
    public ApiResponse<Pipeline> update(@PathVariable String id, @RequestBody UpdatePipelineRequest request) {
        User user = requireCurrentUser();
        Pipeline existing = pipelineService.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineModify(existing, user, permissionService);
        Pipeline updated = pipelineService.updatePipeline(id, request);
        return ApiResponse.ofSuccess(updated);
    }

    /**
     * 删除 Pipeline。
     *
     * @param id Pipeline ID
     * @return 空 data 的成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除Pipeline")
    public ApiResponse<Void> delete(@PathVariable String id) {
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineDelete(pipeline, user, permissionService);
        pipelineService.deletePipeline(id);
        return ApiResponse.ofSuccess();
    }

    /**
     * 触发 Pipeline 异步执行。
     *
     * @param id Pipeline ID
     * @return 初始 {@link ExecutionRun}（多为 PENDING）
     */
    @PostMapping("/{id}/run")
    @Operation(summary = "执行Pipeline")
    public ApiResponse<ExecutionRun> run(@PathVariable String id) {
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineExecute(pipeline, user, permissionService);
        ExecutionRun run = pipelineService.executePipeline(id, user.getId());
        return ApiResponse.ofSuccess(run);
    }

    /**
     * 查询该 Pipeline 下全部执行记录。
     *
     * @param id Pipeline ID
     * @return 执行记录列表
     */
    @GetMapping("/{id}/runs")
    @Operation(summary = "查询Pipeline执行记录")
    public ApiResponse<List<ExecutionRun>> getRuns(@PathVariable String id) {
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineAccess(pipeline, user, permissionService);
        List<ExecutionRun> runs = pipelineService.findExecutionRuns(id);
        return ApiResponse.ofSuccess(runs);
    }

    /**
     * 采样预览 Pipeline 转换结果（不落库）。
     *
     * @param id Pipeline ID
     * @return 预览结果 Map（含样本记录等）
     */
    @GetMapping("/{id}/preview")
    @Operation(summary = "预览Pipeline转换结果")
    public ApiResponse<Map<String, Object>> preview(@PathVariable String id) {
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineAccess(pipeline, user, permissionService);
        // 固定采样 10 条，与文档约定一致
        Map<String, Object> result = pipelineService.previewTransform(pipeline, 10);
        return ApiResponse.ofSuccess(result);
    }

    /**
     * 加载当前登录用户。
     *
     * @return 用户实体
     */
    private User requireCurrentUser() {
        return userService.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
