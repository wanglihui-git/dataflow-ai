package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.FieldPermissionRepository;
import com.dataflow.ai.business.repository.RowPermissionRepository;
import com.dataflow.ai.business.service.DataPermissionService;
import com.dataflow.ai.domain.entity.DataFieldPermission;
import com.dataflow.ai.domain.entity.DataRowPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataPermissionServiceImpl implements DataPermissionService {

    private final FieldPermissionRepository fieldPermissionRepository;
    private final RowPermissionRepository rowPermissionRepository;

    @Override
    public List<DataFieldPermission> listColumnPermissions(String dataSourceId) {
        return fieldPermissionRepository.findByDataSourceId(dataSourceId);
    }

    @Override
    public DataFieldPermission saveColumnPermission(DataFieldPermission permission) {
        return fieldPermissionRepository.save(permission);
    }

    @Override
    public void deleteColumnPermission(String id) {
        fieldPermissionRepository.deleteById(id);
    }

    @Override
    public List<DataRowPermission> listRowPermissions(String dataSourceId) {
        return rowPermissionRepository.findByDataSourceId(dataSourceId);
    }

    @Override
    public DataRowPermission saveRowPermission(DataRowPermission permission) {
        return rowPermissionRepository.save(permission);
    }

    @Override
    public void deleteRowPermission(String id) {
        rowPermissionRepository.deleteById(id);
    }
}
