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

@RestController
@RequiredArgsConstructor
@Tag(name = "数据权限", description = "字段/行级权限配置")
public class DataPermissionController {

    private final DataPermissionService dataPermissionService;
    private final DataSourceService dataSourceService;
    private final UserService userService;
    private final PermissionService permissionService;

    @GetMapping("/v1/data-sources/{dataSourceId}/column-permissions")
    @Operation(summary = "列权限列表")
    public ApiResponse<List<DataFieldPermission>> listColumnPermissions(@PathVariable String dataSourceId) {
        requireDataSourceAccess(dataSourceId);
        return ApiResponse.ofSuccess(dataPermissionService.listColumnPermissions(dataSourceId));
    }

    @PostMapping("/v1/data-sources/{dataSourceId}/column-permissions")
    @Operation(summary = "创建列权限")
    public ApiResponse<DataFieldPermission> createColumnPermission(
            @PathVariable String dataSourceId, @RequestBody DataFieldPermission permission) {
        requireDataSourceModify(dataSourceId);
        permission.setDataSourceId(dataSourceId);
        return ApiResponse.ofSuccess(dataPermissionService.saveColumnPermission(permission));
    }

    @DeleteMapping("/v1/data-sources/{dataSourceId}/column-permissions/{id}")
    @Operation(summary = "删除列权限")
    public ApiResponse<Void> deleteColumnPermission(@PathVariable String dataSourceId, @PathVariable String id) {
        requireDataSourceModify(dataSourceId);
        dataPermissionService.deleteColumnPermission(id);
        return ApiResponse.ofSuccess();
    }

    @GetMapping("/v1/data-sources/{dataSourceId}/row-permissions")
    @Operation(summary = "行权限列表")
    public ApiResponse<List<DataRowPermission>> listRowPermissions(@PathVariable String dataSourceId) {
        requireDataSourceAccess(dataSourceId);
        return ApiResponse.ofSuccess(dataPermissionService.listRowPermissions(dataSourceId));
    }

    @PostMapping("/v1/data-sources/{dataSourceId}/row-permissions")
    @Operation(summary = "创建行权限")
    public ApiResponse<DataRowPermission> createRowPermission(
            @PathVariable String dataSourceId, @RequestBody DataRowPermission permission) {
        requireDataSourceModify(dataSourceId);
        permission.setDataSourceId(dataSourceId);
        return ApiResponse.ofSuccess(dataPermissionService.saveRowPermission(permission));
    }

    @DeleteMapping("/v1/data-sources/{dataSourceId}/row-permissions/{id}")
    @Operation(summary = "删除行权限")
    public ApiResponse<Void> deleteRowPermission(@PathVariable String dataSourceId, @PathVariable String id) {
        requireDataSourceModify(dataSourceId);
        dataPermissionService.deleteRowPermission(id);
        return ApiResponse.ofSuccess();
    }

    private void requireDataSourceAccess(String dataSourceId) {
        User user = requireUser();
        DataSource ds = dataSourceService.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        ResourceAuthorizationHelper.requireDataSourceAccess(ds, user, permissionService);
    }

    private void requireDataSourceModify(String dataSourceId) {
        User user = requireUser();
        DataSource ds = dataSourceService.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        ResourceAuthorizationHelper.requireDataSourceModify(ds, user, permissionService);
    }

    private User requireUser() {
        return userService.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
