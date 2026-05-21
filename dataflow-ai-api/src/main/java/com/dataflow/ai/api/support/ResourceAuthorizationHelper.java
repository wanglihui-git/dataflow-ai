package com.dataflow.ai.api.support;

import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.exception.BusinessException;
import com.dataflow.ai.domain.response.ResponseCode;

/**
 * Controller 层资源鉴权辅助
 */
public final class ResourceAuthorizationHelper {

    private ResourceAuthorizationHelper() {
    }

    public static void requireDataSourceAccess(DataSource dataSource, User user, PermissionService permissionService) {
        if (!permissionService.canAccessDataSource(dataSource, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权访问该数据源");
        }
    }

    public static void requireDataSourceModify(DataSource dataSource, User user, PermissionService permissionService) {
        if (!permissionService.canModifyDataSource(dataSource, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权修改该数据源");
        }
    }

    public static void requirePipelineAccess(Pipeline pipeline, User user, PermissionService permissionService) {
        if (!permissionService.hasPipelineAccess(pipeline, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权访问该 Pipeline");
        }
    }

    public static void requirePipelineModify(Pipeline pipeline, User user, PermissionService permissionService) {
        if (!permissionService.canModifyPipeline(pipeline, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权修改该 Pipeline");
        }
    }

    public static void requirePipelineExecute(Pipeline pipeline, User user, PermissionService permissionService) {
        if (!permissionService.canExecutePipeline(pipeline, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权执行该 Pipeline");
        }
    }

    public static void requirePipelineDelete(Pipeline pipeline, User user, PermissionService permissionService) {
        if (!permissionService.canDeletePipeline(pipeline, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权删除该 Pipeline");
        }
    }
}
