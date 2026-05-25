package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.ResourceAuthorizationHelper;
import com.dataflow.ai.business.service.DataPermissionService;
import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.common.utils.SecurityUtils;
import com.dataflow.ai.domain.entity.DataFieldPermission;
import com.dataflow.ai.domain.entity.DataRowPermission;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.response.ApiResponse;
import com.dataflow.ai.business.service.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据权限 REST 控制器。
 * <p>
 * 为指定数据源配置列级（脱敏/访问类型）与行级（过滤条件）权限规则。
 * 路径挂在 {@code /v1/data-sources/{dataSourceId}/...} 下，写操作需数据源修改权。
 * </p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "数据权限", description = "字段/行级权限配置")
public class DataPermissionController {

    private final DataPermissionService dataPermissionService;
    private final DataSourceService dataSourceService;
    private final UserService userService;
    private final PermissionService permissionService;

    /**
     * 查询数据源的列权限规则列表。
     *
     * @param dataSourceId 数据源 ID
     * @return 列权限实体列表
     */
    @GetMapping("/v1/data-sources/{dataSourceId}/column-permissions")
    @Operation(summary = "列权限列表")
    public ApiResponse<List<DataFieldPermission>> listColumnPermissions(@PathVariable String dataSourceId) {
        requireDataSourceAccess(dataSourceId);
        return ApiResponse.ofSuccess(dataPermissionService.listColumnPermissions(dataSourceId));
    }

    /**
     * 创建或保存列权限规则。
     *
     * @param dataSourceId 数据源 ID
     * @param permission   列名、目标角色/用户、访问类型与脱敏规则等
     * @return 保存后的列权限实体
     */
    @PostMapping("/v1/data-sources/{dataSourceId}/column-permissions")
    @Operation(summary = "创建列权限")
    public ApiResponse<DataFieldPermission> createColumnPermission(
            @PathVariable String dataSourceId, @RequestBody DataFieldPermission permission) {
        requireDataSourceModify(dataSourceId);
        permission.setDataSourceId(dataSourceId);
        return ApiResponse.ofSuccess(dataPermissionService.saveColumnPermission(permission));
    }

    /**
     * 删除列权限规则。
     *
     * @param dataSourceId 数据源 ID
     * @param id           权限记录 ID
     * @return 空 data 的成功响应
     */
    @DeleteMapping("/v1/data-sources/{dataSourceId}/column-permissions/{id}")
    @Operation(summary = "删除列权限")
    public ApiResponse<Void> deleteColumnPermission(@PathVariable String dataSourceId, @PathVariable String id) {
        requireDataSourceModify(dataSourceId);
        dataPermissionService.deleteColumnPermission(id);
        return ApiResponse.ofSuccess();
    }

    /**
     * 查询数据源的行权限规则列表。
     *
     * @param dataSourceId 数据源 ID
     * @return 行权限实体列表
     */
    @GetMapping("/v1/data-sources/{dataSourceId}/row-permissions")
    @Operation(summary = "行权限列表")
    public ApiResponse<List<DataRowPermission>> listRowPermissions(@PathVariable String dataSourceId) {
        requireDataSourceAccess(dataSourceId);
        return ApiResponse.ofSuccess(dataPermissionService.listRowPermissions(dataSourceId));
    }

    /**
     * 创建或保存行权限规则。
     *
     * @param dataSourceId 数据源 ID
     * @param permission   过滤条件、目标角色/用户、优先级等
     * @return 保存后的行权限实体
     */
    @PostMapping("/v1/data-sources/{dataSourceId}/row-permissions")
    @Operation(summary = "创建行权限")
    public ApiResponse<DataRowPermission> createRowPermission(
            @PathVariable String dataSourceId, @RequestBody DataRowPermission permission) {
        requireDataSourceModify(dataSourceId);
        permission.setDataSourceId(dataSourceId);
        return ApiResponse.ofSuccess(dataPermissionService.saveRowPermission(permission));
    }

    /**
     * 删除行权限规则。
     *
     * @param dataSourceId 数据源 ID
     * @param id           权限记录 ID
     * @return 空 data 的成功响应
     */
    @DeleteMapping("/v1/data-sources/{dataSourceId}/row-permissions/{id}")
    @Operation(summary = "删除行权限")
    public ApiResponse<Void> deleteRowPermission(@PathVariable String dataSourceId, @PathVariable String id) {
        requireDataSourceModify(dataSourceId);
        dataPermissionService.deleteRowPermission(id);
        return ApiResponse.ofSuccess();
    }

    /**
     * 校验当前用户对数据源具备访问权。
     *
     * @param dataSourceId 数据源 ID
     */
    private void requireDataSourceAccess(String dataSourceId) {
        User user = requireUser();
        DataSource ds = dataSourceService.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        ResourceAuthorizationHelper.requireDataSourceAccess(ds, user, permissionService);
    }

    /**
     * 校验当前用户对数据源具备修改权。
     *
     * @param dataSourceId 数据源 ID
     */
    private void requireDataSourceModify(String dataSourceId) {
        User user = requireUser();
        DataSource ds = dataSourceService.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        ResourceAuthorizationHelper.requireDataSourceModify(ds, user, permissionService);
    }

    /**
     * 获取当前登录用户。
     *
     * @return 用户实体
     */
    private User requireUser() {
        return userService.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
