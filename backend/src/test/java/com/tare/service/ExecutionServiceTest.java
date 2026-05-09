package com.tare.service;

import com.tare.model.*;
import com.tare.store.MockDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExecutionServiceTest {

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private MockDataStore dataStore;

    @BeforeEach
    void setUp() {
        // 确保有可用的 example.com 数据源和推送接口（使用 Mock 不发起真实请求）
        DataSourceInterface ds = dataStore.getDataSourceInterface("ds-001");
        PushInterface push = dataStore.getPushInterface("push-001");
        if (ds != null && push != null) {
            ds.setUrl("https://api.example.com/users");
            dataStore.saveDataSourceInterface(ds);
            push.setUrl("https://api.example.com/notify");
            dataStore.savePushInterface(push);
        }
    }

    @Test
    void executeBinding_withExampleComUrls_succeedsAndRecordsLog() {
        InterfaceBinding binding = dataStore.getBinding("bind-001");
        assertNotNull(binding);

        int logCountBefore = dataStore.getExecutionLogs().size();
        executionService.executeBinding(binding);
        int logCountAfter = dataStore.getExecutionLogs().size();

        assertEquals(logCountBefore + 1, logCountAfter);

        var latestLog = dataStore.getExecutionLogs().get(0);
        assertEquals("SUCCESS", latestLog.getStatus());
        assertEquals(200, latestLog.getDataSourceStatus());
        assertEquals(200, latestLog.getPushStatus());
        assertNotNull(latestLog.getDataSourceResponse());
        assertNotNull(latestLog.getPushResponse());
    }

    @Test
    void executeBinding_preservesCreatedAtOnBinding() {
        InterfaceBinding binding = dataStore.getBinding("bind-001");
        assertNotNull(binding);
        var originalCreatedAt = binding.getCreatedAt();

        executionService.executeBinding(binding);

        var updated = dataStore.getBinding("bind-001");
        assertEquals(originalCreatedAt, updated.getCreatedAt());
        assertNotNull(updated.getLastExecutedAt());
    }
}
