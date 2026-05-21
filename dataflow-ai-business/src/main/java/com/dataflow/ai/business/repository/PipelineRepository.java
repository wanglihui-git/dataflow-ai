package com.dataflow.ai.business.repository;

import com.dataflow.ai.domain.entity.Pipeline;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Pipeline Repository接口
 */
public interface PipelineRepository {

    /**
     * 根据ID查询Pipeline
     */
    Optional<Pipeline> findById(String id);

    /**
     * 根据所有者ID查询Pipeline列表
     */
    List<Pipeline> findByOwnerId(String ownerId);

    /**
     * 根据权限级别查询公开/共享的Pipeline
     */
    List<Pipeline> findByPermissionLevel(Pipeline.PermissionLevel permissionLevel);

    /**
     * 查询用户有权限访问的Pipeline
     */
    List<Pipeline> findByUser(String userId, String role, String department);

    /**
     * 分页查询用户可访问的 Pipeline（可选名称筛选）
     */
    Page<Pipeline> findAccessiblePage(String userId, String role, String department, String name, Pageable pageable);

    /**
     * 查询所有Pipeline
     */
    List<Pipeline> findAll();

    /**
     * 保存Pipeline
     */
    Pipeline save(Pipeline pipeline);

    /**
     * 删除Pipeline
     */
    void deleteById(String id);

    /**
     * 根据名称查询Pipeline
     */
    Optional<Pipeline> findByName(String name);

    /**
     * 根据状态查询Pipeline
     */
    List<Pipeline> findByStatus(String status);
}
