package com.tare.store;

import com.tare.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MockDataStoreTest {

    @Autowired
    private MockDataStore dataStore;

    @Test
    void savePushInterface_createsNewWithIdAndCreatedAt() {
        PushInterface pi = new PushInterface();
        pi.setName("测试推送");
        pi.setUrl("https://example.com/api");
        pi.setMethod("POST");

        PushInterface saved = dataStore.savePushInterface(pi);

        assertNotNull(saved.getId());
        assertTrue(saved.getId().startsWith("push-"));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void saveDataSourceInterface_createsNewWithIdAndCreatedAt() {
        DataSourceInterface ds = new DataSourceInterface();
        ds.setName("测试数据源");
        ds.setUrl("https://example.com/data");
        ds.setMethod("GET");

        DataSourceInterface saved = dataStore.saveDataSourceInterface(ds);

        assertNotNull(saved.getId());
        assertTrue(saved.getId().startsWith("ds-"));
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void saveBinding_createsNewWithIdAndCreatedAt() {
        InterfaceBinding b = new InterfaceBinding();
        b.setName("测试绑定");
        b.setDataSourceId("ds-001");
        b.setPushInterfaceId("push-001");
        b.setFieldBindings(new ArrayList<>());

        InterfaceBinding saved = dataStore.saveBinding(b);

        assertNotNull(saved.getId());
        assertTrue(saved.getId().startsWith("bind-"));
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void addExecutionLog_generatesIdWhenNull() {
        ExecutionLog log = new ExecutionLog();
        log.setBindingId("bind-001");
        log.setStatus("SUCCESS");

        dataStore.addExecutionLog(log);

        assertNotNull(log.getId());
        assertTrue(log.getId().startsWith("log-"));
    }
}
