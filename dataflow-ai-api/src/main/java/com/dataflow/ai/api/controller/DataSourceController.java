package com.dataflow.ai.api.controller;

import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.domain.response.ApiResponse;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/data-sources")
@RequiredArgsConstructor
@Tag(name = "数据源", description = "数据源管理相关接口")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @PostMapping
    @Operation(summary = "创建数据源")
    public ApiResponse<DataSource> create(
            @RequestParam String name,
            @RequestParam DataSourceType type,
            @RequestBody Map<String, Object> connectionConfig) {
        log.info("Create datasource: {}, type: {}", name, type);
        // TODO: 从上下文获取当前用户ID
        String userId = "user_admin";
        DataSource dataSource = dataSourceService.createDataSource(name, type, connectionConfig, userId);
        return ApiResponse.ofSuccess(dataSource);
    }

    @GetMapping
    @Operation(summary = "查询数据源列表")
    public ApiResponse<List<DataSource>> list() {
        // TODO: 从上下文获取当前用户ID
        String userId = "user_admin";
        List<DataSource> dataSources = dataSourceService.findByCreatedBy(userId);
        return ApiResponse.ofSuccess(dataSources);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询数据源详情")
    public ApiResponse<DataSource> get(@PathVariable String id) {
        DataSource dataSource = dataSourceService.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        return ApiResponse.ofSuccess(dataSource);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据源")
    public ApiResponse<Void> delete(@PathVariable String id) {
        dataSourceService.deleteDataSource(id);
        return ApiResponse.ofSuccess();
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试数据源连接")
    public ApiResponse<Boolean> test(@PathVariable String id) {
        boolean result = dataSourceService.testConnection(id);
        return ApiResponse.ofSuccess(result);
    }

    @PostMapping("/{id}/preview")
    @Operation(summary = "预览数据源数据")
    public ApiResponse<Map<String, Object>> preview(
            @PathVariable String id,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "10") int sampleSize) {
        Map<String, Object> result = dataSourceService.previewSourceData(id, tableName, query, sampleSize);
        return ApiResponse.ofSuccess(result);
    }
}
