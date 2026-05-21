package com.dataflow.ai.business.repository.jpa;

import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExecutionRunJpaRepository extends JpaRepository<ExecutionRun, String> {

    List<ExecutionRun> findByPipelineId(String pipelineId);

    List<ExecutionRun> findByPipelineIdAndStatus(String pipelineId, ExecutionStatus status);

    List<ExecutionRun> findByTriggeredBy(String triggeredBy);

    List<ExecutionRun> findByStatus(ExecutionStatus status);

    org.springframework.data.domain.Page<ExecutionRun> findByStatus(
            ExecutionStatus status, org.springframework.data.domain.Pageable pageable);

    long countByPipelineId(String pipelineId);

    long countByPipelineIdAndStatus(String pipelineId, ExecutionStatus status);

    @Query("SELECT e FROM ExecutionRun e WHERE e.pipelineId = :pipelineId ORDER BY e.startTime DESC LIMIT 1")
    Optional<ExecutionRun> findLatestByPipelineId(@Param("pipelineId") String pipelineId);
}
