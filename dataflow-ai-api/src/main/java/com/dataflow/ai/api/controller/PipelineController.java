package com.dataflow.ai.api.controller;

import com.dataflow.ai.domain.request.CreatePipelineRequest;
import com.dataflow.ai.business.service.PipelineService;
import com.dataflow.ai.common.dto.ApiResponse;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
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
@RequestMapping("/api/v1/pipelines")
@RequiredArgsConstructor
@Tag(name = "Pipeline", description = "Pipeline管理相关接口")
public class PipelineController {

    private final PipelineService pipelineService;

    @PostMapping
    @Operation(summary = "创建Pipeline")
    public ApiResponse<Pipeline> create(@Valid @RequestBody CreatePipelineRequest request) {
        log.info("Create pipeline: {}", request.getName());
        // TODO: 从上下文获取当前用户ID
        String userId = "user_admin";
        Pipeline pipeline = pipelineService.createPipeline(request, userId);
        return ApiResponse.ofSuccess(pipeline);
    }

    @GetMapping
    @Operation(summary = "查询Pipeline列表")
    public ApiResponse<List<Pipeline>> list(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // TODO: 从上下文获取当前用户ID
        String userId = "user_admin";
        List<Pipeline> pipelines = pipelineService.findByUser(userId);
        return ApiResponse.ofSuccess(pipelines);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询Pipeline详情")
    public ApiResponse<Pipeline> get(@PathVariable String id) {
        Pipeline pipeline = pipelineService.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        return ApiResponse.ofSuccess(pipeline);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新Pipeline")
    public ApiResponse<Pipeline> update(@PathVariable String id, @RequestBody Pipeline pipeline) {
        Pipeline updated = pipelineService.updatePipeline(id, pipeline);
        return ApiResponse.ofSuccess(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除Pipeline")
    public ApiResponse<Void> delete(@PathVariable String id) {
        pipelineService.deletePipeline(id);
        return ApiResponse.ofSuccess();
    }

    @PostMapping("/{id}/run")
    @Operation(summary = "执行Pipeline")
    public ApiResponse<ExecutionRun> run(@PathVariable String id) {
        // TODO: 从上下文获取当前用户ID
        String userId = "user_admin";
        ExecutionRun run = pipelineService.executePipeline(id, userId);
        return ApiResponse.ofSuccess(run);
    }

    @GetMapping("/{id}/runs")
    @Operation(summary = "查询Pipeline执行记录")
    public ApiResponse<List<ExecutionRun>> getRuns(@PathVariable String id) {
        List<ExecutionRun> runs = pipelineService.findExecutionRuns(id);
        return ApiResponse.ofSuccess(runs);
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "预览Pipeline转换结果")
    public ApiResponse<Map<String, Object>> preview(@PathVariable String id) {
        Pipeline pipeline = pipelineService.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline不存在"));
        Map<String, Object> result = pipelineService.previewTransform(pipeline, 10);
        return ApiResponse.ofSuccess(result);
    }
}
