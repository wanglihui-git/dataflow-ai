package com.dataflow.ai.domain.entity;

import com.dataflow.ai.domain.converter.TransformListConverter;
import com.dataflow.ai.domain.vo.Transform;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 历史指令模式（instruction_patterns 表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "instruction_patterns")
public class InstructionPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instruction_hash", nullable = false, unique = true, length = 64)
    private String instructionHash;

    @Column(name = "instruction_text", nullable = false, columnDefinition = "text")
    private String instructionText;

    @Convert(converter = TransformListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transform_template", nullable = false, columnDefinition = "jsonb")
    private List<Transform> transformTemplate;

    @Column(name = "use_count")
    @Builder.Default
    private Integer useCount = 1;

    @Column(name = "avg_embedding")
    private float[] avgEmbedding;

    @Column(name = "acceptance_rate", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal acceptanceRate = BigDecimal.ZERO;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
