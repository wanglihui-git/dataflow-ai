package com.dataflow.ai.domain.entity;

import com.dataflow.ai.domain.enums.DataSourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据源实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "data_sources")
public class DataSource {

    /**
     * 数据源ID
     */
    @Id
    private String id;

    /**
     * 数据源名称
     */
    private String name;

    /**
     * 数据源类型
     */
    @Enumerated(EnumType.STRING)
    private DataSourceType type;

    /**
     * 连接配置（加密存储）
     */
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> connectionConfig;

    /**
     * 创建者ID
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
