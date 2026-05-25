package com.dataflow.ai.business.repository.jpa;

import com.dataflow.ai.domain.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Pipeline Spring Data JPA 仓储
 */
public interface PipelineJpaRepository extends JpaRepository<Pipeline, String> {

    List<Pipeline> findByOwnerId(String ownerId);

    List<Pipeline> findByPermissionLevel(Pipeline.PermissionLevel permissionLevel);

    Optional<Pipeline> findByName(String name);

    List<Pipeline> findByStatus(String status);

}
