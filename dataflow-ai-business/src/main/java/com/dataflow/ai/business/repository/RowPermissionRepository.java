package com.dataflow.ai.business.repository;

import com.dataflow.ai.domain.entity.DataRowPermission;

import java.util.List;
import java.util.Optional;

public interface RowPermissionRepository {

    List<DataRowPermission> findByDataSourceId(String dataSourceId);

    Optional<DataRowPermission> findById(String id);

    DataRowPermission save(DataRowPermission permission);

    void deleteById(String id);
}
