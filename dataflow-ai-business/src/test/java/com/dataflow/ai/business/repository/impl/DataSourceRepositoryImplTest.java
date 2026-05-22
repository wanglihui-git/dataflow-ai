package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.jpa.DataSourceJpaRepository;
import com.dataflow.ai.domain.entity.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DataSourceRepositoryImpl 委托单测。
 */

@ExtendWith(MockitoExtension.class)
class DataSourceRepositoryImplTest {

    @Mock
    private DataSourceJpaRepository jpaRepository;

    @InjectMocks
    private DataSourceRepositoryImpl dataSourceRepository;

    /**
     * 验证：findByCreatedBy - 委托 JPA。
     */
    @Test
    @DisplayName("findByCreatedBy - 委托 JPA")
    void findByCreatedBy_delegates() {
        dataSourceRepository.findByCreatedBy("user-001");

        // 断言：校验响应或交互
        verify(jpaRepository).findByCreatedBy("user-001");
    }

    /**
     * 验证：save - 设置时间戳。
     */
    @Test
    @DisplayName("save - 设置时间戳")
    void save_setsTimestamps() {
        // 准备：配置 Mock 返回值
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DataSource saved = dataSourceRepository.save(DataSource.builder().name("ds").build());

        // 断言：校验响应或交互
        assertNotNull(saved.getId());
        assertNotNull(saved.getUpdatedAt());
    }
}
