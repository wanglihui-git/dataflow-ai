package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.entity.DataFieldPermission;
import com.dataflow.ai.domain.entity.DataRowPermission;

import java.util.List;

public interface DataPermissionService {

    List<DataFieldPermission> listColumnPermissions(String dataSourceId);

    DataFieldPermission saveColumnPermission(DataFieldPermission permission);

    void deleteColumnPermission(String id);

    List<DataRowPermission> listRowPermissions(String dataSourceId);

    DataRowPermission saveRowPermission(DataRowPermission permission);

    void deleteRowPermission(String id);
}
