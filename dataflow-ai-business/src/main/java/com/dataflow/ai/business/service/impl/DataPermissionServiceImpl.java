package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.FieldPermissionRepository;
import com.dataflow.ai.business.repository.RowPermissionRepository;
import com.dataflow.ai.business.service.DataPermissionService;
import com.dataflow.ai.domain.entity.DataFieldPermission;
import com.dataflow.ai.domain.entity.DataRowPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link DataPermissionService} 实现：列/行权限规则的仓储读写。
 */
@Service
@RequiredArgsConstructor
public class DataPermissionServiceImpl implements DataPermissionService {

    private final FieldPermissionRepository fieldPermissionRepository;
    private final RowPermissionRepository rowPermissionRepository;

    /** {@inheritDoc} */
    @Override
    public List<DataFieldPermission> listColumnPermissions(String dataSourceId) {
        return fieldPermissionRepository.findByDataSourceId(dataSourceId);
    }

    /** {@inheritDoc} */
    @Override
    public DataFieldPermission saveColumnPermission(DataFieldPermission permission) {
        return fieldPermissionRepository.save(permission);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteColumnPermission(String id) {
        fieldPermissionRepository.deleteById(id);
    }

    /** {@inheritDoc} */
    @Override
    public List<DataRowPermission> listRowPermissions(String dataSourceId) {
        return rowPermissionRepository.findByDataSourceId(dataSourceId);
    }

    /** {@inheritDoc} */
    @Override
    public DataRowPermission saveRowPermission(DataRowPermission permission) {
        return rowPermissionRepository.save(permission);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteRowPermission(String id) {
        rowPermissionRepository.deleteById(id);
    }
}
