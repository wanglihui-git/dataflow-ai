package com.dataflow.ai.business.repository.jpa;

import com.dataflow.ai.domain.entity.InstructionPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 指令模式 Spring Data JPA 仓储
 */
public interface InstructionPatternJpaRepository extends JpaRepository<InstructionPattern, Long> {

    Optional<InstructionPattern> findByInstructionHash(String instructionHash);

    @Query(value = """
            SELECT * FROM instruction_patterns
            WHERE avg_embedding IS NOT NULL
              AND (avg_embedding <=> CAST(:embedding AS vector)) < :maxDistance
            ORDER BY avg_embedding <=> CAST(:embedding AS vector)
            LIMIT :lim
            """, nativeQuery = true)
    List<InstructionPattern> searchByEmbedding(@Param("embedding") String embedding,
                                               @Param("maxDistance") double maxDistance,
                                               @Param("lim") int lim);
}
