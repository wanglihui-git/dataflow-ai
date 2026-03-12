package com.dataflow.ai.business.repository.jpa;

import com.dataflow.ai.domain.entity.DataFieldPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldPermissionJpaRepository extends JpaRepository<DataFieldPermission, String> {

    List<DataFieldPermission> findByDataSourceId(String dataSourceId);

    Page<DataFieldPermission> findByDataSourceId(String dataSourceId, Pageable pageable);
}
