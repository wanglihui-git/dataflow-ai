package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.ResourceAuthorizationHelper;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.common.utils.SecurityUtils;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.request.CreateDataSourceRequest;
import com.dataflow.ai.domain.request.UpdateDataSourceRequest;
import com.dataflow.ai.domain.response.ApiResponse;
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
    private final UserService userService;
    private final PermissionService permissionService;

    @PostMapping
    @Operation(summary = "创建数据源")
    public ApiResponse<DataSource> create(@RequestBody CreateDataSourceRequest request) {
        log.info("Create datasource: {}, type: {}", request.getName(), request.getType());
        String userId = SecurityUtils.getCurrentUserId();
        DataSource dataSource = dataSourceService.createDataSource(request, userId);
        return ApiResponse.ofSuccess(dataSource);
    }

    @GetMapping
    @Operation(summary = "查询数据源列表")
    public ApiResponse<List<DataSource>> list() {
        String userId = SecurityUtils.getCurrentUserId();
        List<DataSource> dataSources = dataSourceService.findByCreatedBy(userId);
        return ApiResponse.ofSuccess(dataSources);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询数据源详情")
    public ApiResponse<DataSource> get(@PathVariable String id) {
        User user = requireCurrentUser();
        DataSource dataSource = dataSourceService.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        ResourceAuthorizationHelper.requireDataSourceAccess(dataSource, user, permissionService);
        return ApiResponse.ofSuccess(dataSource);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据源")
    public ApiResponse<Void> delete(@PathVariable String id) {
        User user = requireCurrentUser();
        DataSource dataSource = dataSourceService.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        ResourceAuthorizationHelper.requireDataSourceModify(dataSource, user, permissionService);
        dataSourceService.deleteDataSource(id);
        return ApiResponse.ofSuccess();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新数据源")
    public ApiResponse<DataSource> update(
            @PathVariable String id,
            @RequestBody UpdateDataSourceRequest request) {
        log.info("Update datasource: {}", id);
        User user = requireCurrentUser();
        DataSource existing = dataSourceService.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        ResourceAuthorizationHelper.requireDataSourceModify(existing, user, permissionService);
        DataSource dataSource = dataSourceService.updateDataSource(id, request);
        return ApiResponse.ofSuccess(dataSource);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试数据源连接")
    public ApiResponse<Boolean> test(@PathVariable String id) {
        User user = requireCurrentUser();
        DataSource dataSource = dataSourceService.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        ResourceAuthorizationHelper.requireDataSourceAccess(dataSource, user, permissionService);
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
        User user = requireCurrentUser();
        DataSource dataSource = dataSourceService.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        ResourceAuthorizationHelper.requireDataSourceAccess(dataSource, user, permissionService);
        Map<String, Object> result = dataSourceService.previewSourceData(id, tableName, query, sampleSize);
        return ApiResponse.ofSuccess(result);
    }

    private User requireCurrentUser() {
        return userService.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
