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
import com.dataflow.ai.domain.response.ResponseCode;
import com.dataflow.ai.domain.vo.ConnectionTestResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源 REST 控制器。
 * <p>
 * 管理外部数据连接配置（创建、查询、更新、删除、连通性测试与数据预览）。
 * 连接配置在 Service 层加密存储；访问与修改需通过 {@link ResourceAuthorizationHelper} 鉴权。
 * </p>
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

    /**
     * 创建数据源，归属当前登录用户。
     *
     * @param request 名称、类型与连接配置
     * @return 持久化后的数据源实体（连接配置可能为密文）
     */
    @PostMapping
    @Operation(summary = "创建数据源")
    public ApiResponse<DataSource> create(@RequestBody CreateDataSourceRequest request) {
        log.info("Create datasource: {}, type: {}", request.getName(), request.getType());
        String userId = SecurityUtils.getCurrentUserId();
        DataSource dataSource = dataSourceService.createDataSource(request, userId);
        return ApiResponse.ofSuccess(dataSource);
    }

    /**
     * 列出当前用户创建的数据源。
     *
     * @return 数据源列表
     */
    @GetMapping
    @Operation(summary = "查询数据源列表")
    public ApiResponse<List<DataSource>> list() {
        String userId = SecurityUtils.getCurrentUserId();
        List<DataSource> dataSources = dataSourceService.findByCreatedBy(userId);
        return ApiResponse.ofSuccess(dataSources);
    }

    /**
     * 查询数据源详情（需有访问权限）。
     *
     * @param id 数据源 ID
     * @return 数据源实体
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询数据源详情")
    public ApiResponse<DataSource> get(@PathVariable String id) {
        User user = requireCurrentUser();
        DataSource dataSource = dataSourceService.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        ResourceAuthorizationHelper.requireDataSourceAccess(dataSource, user, permissionService);
        return ApiResponse.ofSuccess(dataSource);
    }

    /**
     * 删除数据源（需有修改权限）。
     *
     * @param id 数据源 ID
     * @return 空 data 的成功响应
     */
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

    /**
     * 更新数据源（部分字段，需有修改权限）。
     *
     * @param id      数据源 ID
     * @param request 待更新字段
     * @return 更新后的数据源
     */
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

    /**
     * 测试数据源连通性。
     *
     * @param id 数据源 ID
     * @return 连通性结果；失败时 code 为 400，msg 与 data.message 含失败原因
     */
    @PostMapping("/{id}/test")
    @Operation(summary = "测试数据源连接")
    public ApiResponse<ConnectionTestResult> test(@PathVariable String id) {
        User user = requireCurrentUser();
        DataSource dataSource = dataSourceService.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        ResourceAuthorizationHelper.requireDataSourceAccess(dataSource, user, permissionService);
        ConnectionTestResult result = dataSourceService.testConnection(id);
        if (result.isConnected()) {
            return ApiResponse.ofSuccess(result);
        }
        return ApiResponse.of(ResponseCode.CODE_400.getCode(), result.getMessage(), result);
    }

    /**
     * 预览源端数据样本。
     *
     * @param id         数据源 ID
     * @param tableName  表名（可选）
     * @param query      自定义查询（可选）
     * @param sampleSize 采样条数，默认 10
     * @return 列信息与样本行等（结构由 Reader 决定）
     */
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

    /**
     * 加载当前 JWT 对应用户，不存在则抛异常。
     *
     * @return 当前用户实体
     */
    private User requireCurrentUser() {
        return userService.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
