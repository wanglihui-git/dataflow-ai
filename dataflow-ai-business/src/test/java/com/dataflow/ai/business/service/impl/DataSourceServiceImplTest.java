package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.engine.source.SourceReader;
import com.dataflow.ai.business.engine.source.SourceReaderFactory;
import com.dataflow.ai.business.repository.DataSourceRepository;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.request.CreateDataSourceRequest;
import com.dataflow.ai.domain.request.UpdateDataSourceRequest;
import com.dataflow.ai.domain.vo.ConnectionTestResult;
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
import org.mockito.ArgumentCaptor;

/**
 * DataSourceServiceImpl 创建、连接测试与预览单测。
 */

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

    /**
     * 验证：createDataSource - 加密并保存。
     */
    @Test
    @DisplayName("createDataSource - 加密并保存")
    void createDataSource_encryptsAndSaves() {
        // 准备：配置 Mock 返回值
        when(encryptionService.encrypt(anyMap())).thenReturn(Map.of("url", "enc"));
        when(dataSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateDataSourceRequest request = CreateDataSourceRequest.builder()
                .name("ds")
                .type(DataSourceType.MYSQL)
                .connectionConfig(Map.of("url", "jdbc:mysql://localhost/db"))
                .build();

        DataSource saved = dataSourceService.createDataSource(request, "user-001");

        assertTrue(saved.getId() != null && !saved.getId().isBlank());
        // 断言：校验响应或交互
        verify(encryptionService).encrypt(anyMap());
        verify(dataSourceRepository).save(any());
    }

    /**
     * 验证：updateDataSource - 部分 connectionConfig 与已有配置合并后再加密。
     */
    @Test
    @DisplayName("updateDataSource - 部分 connectionConfig 合并")
    void updateDataSource_partialConnectionConfig_mergesWithExisting() {
        Map<String, Object> storedEncrypted = Map.of("host", "enc-host", "password", "enc-pwd");
        DataSource existing = DataSource.builder()
                .id("ds-1")
                .name("mysql-demo")
                .type(DataSourceType.MYSQL)
                .connectionConfig(storedEncrypted)
                .build();
        Map<String, Object> existingPlain = new java.util.HashMap<>(Map.of(
                "host", "127.0.0.1",
                "port", 3306,
                "database", "demo",
                "username", "root",
                "password", "111111"));
        when(dataSourceRepository.findById("ds-1")).thenReturn(Optional.of(existing));
        when(encryptionService.decrypt(storedEncrypted)).thenReturn(existingPlain);
        when(encryptionService.encrypt(anyMap())).thenAnswer(inv -> inv.getArgument(0));
        when(dataSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dataSourceService.updateDataSource("ds-1",
                UpdateDataSourceRequest.builder()
                        .connectionConfig(Map.of("password", "123456"))
                        .build());

        ArgumentCaptor<Map<String, Object>> encryptCaptor = ArgumentCaptor.forClass(Map.class);
        verify(encryptionService).decrypt(storedEncrypted);
        verify(encryptionService).encrypt(encryptCaptor.capture());
        Map<String, Object> merged = encryptCaptor.getValue();
        assertEquals("127.0.0.1", merged.get("host"));
        assertEquals(3306, merged.get("port"));
        assertEquals("demo", merged.get("database"));
        assertEquals("root", merged.get("username"));
        assertEquals("123456", merged.get("password"));
    }

    /**
     * 验证：testConnection - 委托 SourceReader。
     */
    @Test
    @DisplayName("testConnection - 委托 SourceReader")
    void testConnection_delegatesToReader() {
        DataSource ds = DataSource.builder()
                .id("ds-1")
                .type(DataSourceType.MYSQL)
                .connectionConfig(Map.of("k", "v"))
                .build();
        // 准备：配置 Mock 返回值
        when(dataSourceRepository.findById("ds-1")).thenReturn(Optional.of(ds));
        when(sourceReaderFactory.createReader(ds)).thenReturn(sourceReader);
        when(sourceReader.testConnection(ds)).thenReturn(ConnectionTestResult.success());

        ConnectionTestResult result = dataSourceService.testConnection("ds-1");
        assertTrue(result.isConnected());
    }

    /**
     * 验证：testConnection - 失败返回 false。
     */
    @Test
    @DisplayName("testConnection - 失败返回 false")
    void testConnection_failureReturnsFalse() {
        DataSource ds = DataSource.builder().id("ds-1").type(DataSourceType.API).build();
        // 准备：配置 Mock 返回值
        when(dataSourceRepository.findById("ds-1")).thenReturn(Optional.of(ds));
        when(sourceReaderFactory.createReader(ds)).thenReturn(sourceReader);
        when(sourceReader.testConnection(ds)).thenReturn(
                ConnectionTestResult.failure("数据库连接失败: Connection refused"));

        ConnectionTestResult result = dataSourceService.testConnection("ds-1");
        assertFalse(result.isConnected());
        assertTrue(result.getMessage().contains("Connection refused"));
    }

    /**
     * 验证：previewSourceData - 返回 columns 与 rows。
     */
    @Test
    @DisplayName("previewSourceData - 返回 columns 与 rows")
    void previewSourceData_returnsColumnsAndRows() throws Exception {
        DataSource ds = DataSource.builder()
                .id("ds-1")
                .type(DataSourceType.MYSQL)
                .build();
        // 准备：配置 Mock 返回值
        when(dataSourceRepository.findById("ds-1")).thenReturn(Optional.of(ds));
        when(sourceReaderFactory.createReader(ds)).thenReturn(sourceReader);

        Record record = new Record();
        record.setId("r1");
        record.set("id", 1);
        record.set("name", "a");
        when(sourceReader.preview(any(SourceConfig.class), anyInt())).thenReturn(List.of(record));

        Map<String, Object> result = dataSourceService.previewSourceData("ds-1", "users", null, 5);

        // 断言：校验响应或交互
        assertEquals(1, result.get("rowCount"));
        assertTrue(((List<?>) result.get("columns")).contains("name"));
    }
}
