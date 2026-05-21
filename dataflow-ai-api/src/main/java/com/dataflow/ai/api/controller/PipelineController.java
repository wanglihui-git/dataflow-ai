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
import com.dataflow.ai.domain.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Pipeline控制器
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

    @PostMapping
    @Operation(summary = "创建Pipeline")
    public ApiResponse<Pipeline> create(@Valid @RequestBody CreatePipelineRequest request) {
        log.info("Create pipeline: {}", request.getName());
        String userId = SecurityUtils.getCurrentUserId();
        Pipeline pipeline = pipelineService.createPipeline(request, userId);
        return ApiResponse.ofSuccess(pipeline);
    }

    @GetMapping
    @Operation(summary = "查询Pipeline列表")
    public ApiResponse<List<Pipeline>> list(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = SecurityUtils.getCurrentUserId();
        List<Pipeline> pipelines = pipelineService.findByUser(userId);
        return ApiResponse.ofSuccess(pipelines);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询Pipeline详情")
    public ApiResponse<Pipeline> get(@PathVariable String id) {
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineAccess(pipeline, user, permissionService);
        return ApiResponse.ofSuccess(pipeline);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新Pipeline")
    public ApiResponse<Pipeline> update(@PathVariable String id, @RequestBody Pipeline pipeline) {
        User user = requireCurrentUser();
        Pipeline existing = pipelineService.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineModify(existing, user, permissionService);
        Pipeline updated = pipelineService.updatePipeline(id, pipeline);
        return ApiResponse.ofSuccess(updated);
    }

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

    @GetMapping("/{id}/preview")
    @Operation(summary = "预览Pipeline转换结果")
    public ApiResponse<Map<String, Object>> preview(@PathVariable String id) {
        User user = requireCurrentUser();
        Pipeline pipeline = pipelineService.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        ResourceAuthorizationHelper.requirePipelineAccess(pipeline, user, permissionService);
        Map<String, Object> result = pipelineService.previewTransform(pipeline, 10);
        return ApiResponse.ofSuccess(result);
    }

    private User requireCurrentUser() {
        return userService.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
