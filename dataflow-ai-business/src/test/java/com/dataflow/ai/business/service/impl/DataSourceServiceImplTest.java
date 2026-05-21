package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.engine.source.SourceReader;
import com.dataflow.ai.business.engine.source.SourceReaderFactory;
import com.dataflow.ai.business.repository.DataSourceRepository;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.request.CreateDataSourceRequest;
import com.dataflow.ai.domain.vo.SourceConfig;
import com.dataflow.ai.infrastructure.security.EncryptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSourceServiceImplTest {

    @Mock
    private DataSourceRepository dataSourceRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private SourceReaderFactory sourceReaderFactory;

    @Mock
    private SourceReader sourceReader;

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
    @DisplayName("testConnection - 委托 SourceReader")
    void testConnection_delegatesToReader() {
        DataSource ds = DataSource.builder()
                .id("ds-1")
                .type(DataSourceType.MYSQL)
                .connectionConfig(Map.of("k", "v"))
                .build();
        when(dataSourceRepository.findById("ds-1")).thenReturn(Optional.of(ds));
        when(sourceReaderFactory.createReader(ds)).thenReturn(sourceReader);
        when(sourceReader.testConnection(ds)).thenReturn(true);

        assertTrue(dataSourceService.testConnection("ds-1"));
    }

    @Test
    @DisplayName("testConnection - 失败返回 false")
    void testConnection_failureReturnsFalse() {
        DataSource ds = DataSource.builder().id("ds-1").type(DataSourceType.API).build();
        when(dataSourceRepository.findById("ds-1")).thenReturn(Optional.of(ds));
        when(sourceReaderFactory.createReader(ds)).thenReturn(sourceReader);
        when(sourceReader.testConnection(ds)).thenReturn(false);

        assertFalse(dataSourceService.testConnection("ds-1"));
    }

    @Test
    @DisplayName("previewSourceData - 返回 columns 与 rows")
    void previewSourceData_returnsColumnsAndRows() throws Exception {
        DataSource ds = DataSource.builder()
                .id("ds-1")
                .type(DataSourceType.MYSQL)
                .build();
        when(dataSourceRepository.findById("ds-1")).thenReturn(Optional.of(ds));
        when(sourceReaderFactory.createReader(ds)).thenReturn(sourceReader);

        Record record = new Record();
        record.setId("r1");
        record.set("id", 1);
        record.set("name", "a");
        when(sourceReader.preview(any(SourceConfig.class), anyInt())).thenReturn(List.of(record));

        Map<String, Object> result = dataSourceService.previewSourceData("ds-1", "users", null, 5);

        assertEquals(1, result.get("rowCount"));
        assertTrue(((List<?>) result.get("columns")).contains("name"));
    }
}
