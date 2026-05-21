package com.dataflow.ai.business.repository.jpa;

import com.dataflow.ai.domain.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PipelineJpaRepository extends JpaRepository<Pipeline, String> {

    List<Pipeline> findByOwnerId(String ownerId);

    List<Pipeline> findByPermissionLevel(Pipeline.PermissionLevel permissionLevel);

    Optional<Pipeline> findByName(String name);

    List<Pipeline> findByStatus(String status);

    /**
     * 查找用户有权访问的 Pipeline：
     * 1. 用户是 owner
     * 2. Pipeline 为 PUBLIC
     * 3. 用户在 allowed_users 列表中（JSONB 数组包含查询）
     */
}
