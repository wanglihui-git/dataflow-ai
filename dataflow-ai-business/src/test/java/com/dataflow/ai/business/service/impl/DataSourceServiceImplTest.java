package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.DataSourceRepository;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.request.CreateDataSourceRequest;
import com.dataflow.ai.infrastructure.security.EncryptionService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSourceServiceImplTest {

    @Mock
    private DataSourceRepository dataSourceRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private DataSourceServiceImpl dataSourceService;

    @Test
    @DisplayName("createDataSource - 加密并保存")
    void createDataSource_encryptsAndSaves() {
        when(encryptionService.encrypt(anyMap())).thenReturn(Map.of("url", "enc"));
        when(dataSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateDataSourceRequest request = CreateDataSourceRequest.builder()
                .name("ds")
                .type(DataSourceType.MYSQL)
                .connectionConfig(Map.of("url", "jdbc:mysql://localhost/db"))
                .build();

        DataSource saved = dataSourceService.createDataSource(request, "user-001");

        assertTrue(saved.getId() != null && !saved.getId().isBlank());
        verify(encryptionService).encrypt(anyMap());
        verify(dataSourceRepository).save(any());
    }

    @Test
    @DisplayName("testConnection - 占位实现恒为 true")
    void testConnection_placeholderReturnsTrue() {
        DataSource ds = DataSource.builder()
                .id("ds-1")
                .connectionConfig(Map.of("k", "v"))
                .build();
        when(dataSourceRepository.findById("ds-1")).thenReturn(Optional.of(ds));
        when(encryptionService.decrypt(anyMap())).thenReturn(Map.of("k", "v"));

        assertTrue(dataSourceService.testConnection("ds-1"));
    }

    @Test
    @Disabled("待实现真实预览逻辑后补充：previewSourceData 应返回 columns/rows")
    void previewSourceData_placeholder() {
    }
}
