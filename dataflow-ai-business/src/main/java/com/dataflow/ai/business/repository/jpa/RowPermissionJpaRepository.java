package com.dataflow.ai.business.repository.jpa;

import com.dataflow.ai.domain.entity.DataRowPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 行权限 Spring Data JPA 仓储
 */
public interface RowPermissionJpaRepository extends JpaRepository<DataRowPermission, String> {

    List<DataRowPermission> findByDataSourceId(String dataSourceId);
}
