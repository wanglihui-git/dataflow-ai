package com.dataflow.ai.api.support;

import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.exception.BusinessException;
import com.dataflow.ai.domain.response.ResponseCode;

/**
 * Controller 层资源鉴权辅助类。
 * <p>
 * 在调用 Service 前统一校验当前用户对数据源、Pipeline 的访问/修改/执行/删除权限，
 * 无权限时抛出 {@link BusinessException}（HTTP 200，body code 403）。
 * </p>
 */
public final class ResourceAuthorizationHelper {

    private ResourceAuthorizationHelper() {
    }

    /**
     * 要求当前用户可访问该数据源（读、测试、预览等）。
     *
     * @param dataSource        数据源实体
     * @param user              当前用户
     * @param permissionService 权限服务
     */
    public static void requireDataSourceAccess(DataSource dataSource, User user, PermissionService permissionService) {
        if (!permissionService.canAccessDataSource(dataSource, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权访问该数据源");
        }
    }

    /**
     * 要求当前用户可修改或删除该数据源。
     *
     * @param dataSource        数据源实体
     * @param user              当前用户
     * @param permissionService 权限服务
     */
    public static void requireDataSourceModify(DataSource dataSource, User user, PermissionService permissionService) {
        if (!permissionService.canModifyDataSource(dataSource, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权修改该数据源");
        }
    }

    /**
     * 要求当前用户可查看该 Pipeline（含执行记录、预览等）。
     *
     * @param pipeline          Pipeline 实体
     * @param user              当前用户
     * @param permissionService 权限服务
     */
    public static void requirePipelineAccess(Pipeline pipeline, User user, PermissionService permissionService) {
        if (!permissionService.hasPipelineAccess(pipeline, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权访问该 Pipeline");
        }
    }

    /**
     * 要求当前用户可更新 Pipeline 配置。
     *
     * @param pipeline          Pipeline 实体
     * @param user              当前用户
     * @param permissionService 权限服务
     */
    public static void requirePipelineModify(Pipeline pipeline, User user, PermissionService permissionService) {
        if (!permissionService.canModifyPipeline(pipeline, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权修改该 Pipeline");
        }
    }

    /**
     * 要求当前用户可触发 Pipeline 执行或取消执行。
     *
     * @param pipeline          Pipeline 实体
     * @param user              当前用户
     * @param permissionService 权限服务
     */
    public static void requirePipelineExecute(Pipeline pipeline, User user, PermissionService permissionService) {
        if (!permissionService.canExecutePipeline(pipeline, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权执行该 Pipeline");
        }
    }

    /**
     * 要求当前用户可删除 Pipeline。
     *
     * @param pipeline          Pipeline 实体
     * @param user              当前用户
     * @param permissionService 权限服务
     */
    public static void requirePipelineDelete(Pipeline pipeline, User user, PermissionService permissionService) {
        if (!permissionService.canDeletePipeline(pipeline, user)) {
            throw new BusinessException(ResponseCode.CODE_403, "无权删除该 Pipeline");
        }
    }
}
