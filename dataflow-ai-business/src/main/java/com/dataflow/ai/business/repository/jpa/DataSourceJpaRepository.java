package com.dataflow.ai.business.repository.jpa;

import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 数据源 Spring Data JPA 仓储
 */
public interface DataSourceJpaRepository extends JpaRepository<DataSource, String> {

    List<DataSource> findByCreatedBy(String createdBy);

    List<DataSource> findByType(DataSourceType type);

    Optional<DataSource> findByName(String name);
}
