package com.tare.store;

import com.tare.model.*;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockDataStore {
    
    private final Map<String, PushInterface> pushInterfaces = new ConcurrentHashMap<>();
    private final Map<String, DataSourceInterface> dataSourceInterfaces = new ConcurrentHashMap<>();
    private final Map<String, InterfaceBinding> bindings = new ConcurrentHashMap<>();
    private final List<ExecutionLog> executionLogs = Collections.synchronizedList(new ArrayList<>());

    @PostConstruct
    public void init() {
        initMockData();
    }

    private void initMockData() {
        // Mock Push Interface 1
        PushInterface push1 = new PushInterface();
        push1.setId("push-001");
        push1.setName("用户通知推送接口");
        push1.setUrl("https://api.example.com/notify");
        push1.setMethod("POST");
        Map<String, String> headers1 = new HashMap<>();
        headers1.put("Content-Type", "application/json");
        headers1.put("Authorization", "Bearer token123");
        push1.setHeaders(headers1);
        
        List<RequestParam> params1 = new ArrayList<>();
        RequestParam p1 = new RequestParam();
        p1.setName("userId");
        p1.setType("string");
        p1.setDescription("用户ID");
        p1.setRequired(true);
        params1.add(p1);
        
        RequestParam p2 = new RequestParam();
        p2.setName("message");
        p2.setType("string");
        p2.setDescription("通知消息");
        p2.setRequired(true);
        params1.add(p2);
        
        RequestParam p3 = new RequestParam();
        p3.setName("type");
        p3.setType("string");
        p3.setDescription("通知类型");
        p3.setRequired(false);
        p3.setDefaultValue("info");
        params1.add(p3);
        
        push1.setRequestParams(params1);
        push1.setEnabled(true);
        push1.setCreatedAt(LocalDateTime.now().minusDays(7));
        push1.setUpdatedAt(LocalDateTime.now());
        pushInterfaces.put(push1.getId(), push1);

        // Mock Push Interface 2
        PushInterface push2 = new PushInterface();
        push2.setId("push-002");
        push2.setName("订单状态更新接口");
        push2.setUrl("https://api.example.com/order/status");
        push2.setMethod("PUT");
        Map<String, String> headers2 = new HashMap<>();
        headers2.put("Content-Type", "application/json");
        push2.setHeaders(headers2);
        
        List<RequestParam> params2 = new ArrayList<>();
        RequestParam op1 = new RequestParam();
        op1.setName("orderId");
        op1.setType("string");
        op1.setDescription("订单ID");
        op1.setRequired(true);
        params2.add(op1);
        
        RequestParam op2 = new RequestParam();
        op2.setName("status");
        op2.setType("string");
        op2.setDescription("订单状态");
        op2.setRequired(true);
        params2.add(op2);
        
        push2.setRequestParams(params2);
        push2.setEnabled(true);
        push2.setCreatedAt(LocalDateTime.now().minusDays(5));
        push2.setUpdatedAt(LocalDateTime.now());
        pushInterfaces.put(push2.getId(), push2);

        // Mock Data Source Interface 1
        DataSourceInterface ds1 = new DataSourceInterface();
        ds1.setId("ds-001");
        ds1.setName("用户信息查询接口");
        ds1.setUrl("https://api.example.com/users");
        ds1.setMethod("GET");
        Map<String, String> dsHeaders1 = new HashMap<>();
        dsHeaders1.put("Accept", "application/json");
        ds1.setHeaders(dsHeaders1);
        ds1.setRequestBody(new HashMap<>());
        
        List<ResponseField> fields1 = new ArrayList<>();
        ResponseField f1 = new ResponseField();
        f1.setPath("data.id");
        f1.setName("用户ID");
        f1.setType("string");
        f1.setDescription("用户唯一标识");
        fields1.add(f1);
        
        ResponseField f2 = new ResponseField();
        f2.setPath("data.name");
        f2.setName("用户名");
        f2.setType("string");
        f2.setDescription("用户名称");
        fields1.add(f2);
        
        ResponseField f3 = new ResponseField();
        f3.setPath("data.email");
        f3.setName("邮箱");
        f3.setType("string");
        f3.setDescription("用户邮箱");
        fields1.add(f3);
        
        ds1.setResponseFields(fields1);
        ds1.setPostProcessor("");
        ds1.setEnabled(true);
        ds1.setCreatedAt(LocalDateTime.now().minusDays(10));
        ds1.setUpdatedAt(LocalDateTime.now());
        dataSourceInterfaces.put(ds1.getId(), ds1);

        // Mock Data Source Interface 2
        DataSourceInterface ds2 = new DataSourceInterface();
        ds2.setId("ds-002");
        ds2.setName("订单列表查询接口");
        ds2.setUrl("https://api.example.com/orders");
        ds2.setMethod("POST");
        Map<String, String> dsHeaders2 = new HashMap<>();
        dsHeaders2.put("Content-Type", "application/json");
        ds2.setHeaders(dsHeaders2);
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("page", 1);
        reqBody.put("size", 10);
        ds2.setRequestBody(reqBody);
        
        List<ResponseField> fields2 = new ArrayList<>();
        ResponseField of1 = new ResponseField();
        of1.setPath("data.orders[*].id");
        of1.setName("订单ID");
        of1.setType("string");
        of1.setDescription("订单唯一标识");
        fields2.add(of1);
        
        ResponseField of2 = new ResponseField();
        of2.setPath("data.orders[*].status");
        of2.setName("订单状态");
        of2.setType("string");
        of2.setDescription("订单当前状态");
        fields2.add(of2);
        
        ds2.setResponseFields(fields2);
        ds2.setPostProcessor("");
        ds2.setEnabled(true);
        ds2.setCreatedAt(LocalDateTime.now().minusDays(8));
        ds2.setUpdatedAt(LocalDateTime.now());
        dataSourceInterfaces.put(ds2.getId(), ds2);

        // Mock Binding
        InterfaceBinding binding1 = new InterfaceBinding();
        binding1.setId("bind-001");
        binding1.setName("用户通知推送任务");
        binding1.setDataSourceId("ds-001");
        binding1.setPushInterfaceId("push-001");
        
        List<FieldBinding> fieldBindings = new ArrayList<>();
        FieldBinding fb1 = new FieldBinding();
        fb1.setPushParamName("userId");
        fb1.setSourceFieldPath("data.id");
        fb1.setDefaultValue("");
        fieldBindings.add(fb1);
        
        FieldBinding fb2 = new FieldBinding();
        fb2.setPushParamName("message");
        fb2.setSourceFieldPath("");
        fb2.setDefaultValue("您有新的通知");
        fieldBindings.add(fb2);
        
        FieldBinding fb3 = new FieldBinding();
        fb3.setPushParamName("type");
        fb3.setSourceFieldPath("");
        fb3.setDefaultValue("info");
        fieldBindings.add(fb3);
        
        binding1.setFieldBindings(fieldBindings);
        binding1.setCronExpression("0 0/5 * * * ?");
        binding1.setEnabled(true);
        binding1.setMaxRetryCount(3);
        binding1.setCreatedAt(LocalDateTime.now().minusDays(3));
        binding1.setUpdatedAt(LocalDateTime.now());
        bindings.put(binding1.getId(), binding1);

        // Mock Execution Logs
        for (int i = 0; i < 5; i++) {
            ExecutionLog log = new ExecutionLog();
            log.setId("log-" + String.format("%03d", i + 1));
            log.setBindingId("bind-001");
            log.setBindingName("用户通知推送任务");
            log.setDataSourceId("ds-001");
            log.setDataSourceName("用户信息查询接口");
            log.setPushInterfaceId("push-001");
            log.setPushInterfaceName("用户通知推送接口");
            log.setDataSourceRequest("{\"url\":\"https://api.example.com/users\",\"method\":\"GET\"}");
            log.setDataSourceResponse("{\"code\":200,\"data\":{\"id\":\"user-123\",\"name\":\"张三\"}}");
            log.setDataSourceStatus(200);
            log.setPushRequest("{\"userId\":\"user-123\",\"message\":\"您有新的通知\",\"type\":\"info\"}");
            log.setPushResponse("{\"code\":200,\"message\":\"success\"}");
            log.setPushStatus(200);
            log.setStatus(i % 3 == 0 ? "FAILED" : "SUCCESS");
            log.setErrorMessage(i % 3 == 0 ? "Connection timeout" : null);
            log.setRetryCount(0);
            log.setExecutedAt(LocalDateTime.now().minusMinutes(i * 5));
            log.setDuration(100L + i * 50);
            executionLogs.add(log);
        }
    }

    public List<PushInterface> getAllPushInterfaces() {
        return new ArrayList<>(pushInterfaces.values());
    }

    public PushInterface getPushInterface(String id) {
        return pushInterfaces.get(id);
    }

    public PushInterface savePushInterface(PushInterface pi) {
        if (pi.getId() == null || pi.getId().isEmpty()) {
            pi.setId("push-" + UUID.randomUUID().toString().substring(0, 8));
            pi.setCreatedAt(LocalDateTime.now());
        }
        pi.setUpdatedAt(LocalDateTime.now());
        pushInterfaces.put(pi.getId(), pi);
        return pi;
    }

    public void deletePushInterface(String id) {
        pushInterfaces.remove(id);
    }

    public List<DataSourceInterface> getAllDataSourceInterfaces() {
        return new ArrayList<>(dataSourceInterfaces.values());
    }

    public DataSourceInterface getDataSourceInterface(String id) {
        return dataSourceInterfaces.get(id);
    }

    public DataSourceInterface saveDataSourceInterface(DataSourceInterface ds) {
        if (ds.getId() == null || ds.getId().isEmpty()) {
            ds.setId("ds-" + UUID.randomUUID().toString().substring(0, 8));
            ds.setCreatedAt(LocalDateTime.now());
        }
        ds.setUpdatedAt(LocalDateTime.now());
        dataSourceInterfaces.put(ds.getId(), ds);
        return ds;
    }

    public void deleteDataSourceInterface(String id) {
        dataSourceInterfaces.remove(id);
    }

    public List<InterfaceBinding> getAllBindings() {
        return new ArrayList<>(bindings.values());
    }

    public InterfaceBinding getBinding(String id) {
        return bindings.get(id);
    }

    public InterfaceBinding saveBinding(InterfaceBinding b) {
        if (b.getId() == null || b.getId().isEmpty()) {
            b.setId("bind-" + UUID.randomUUID().toString().substring(0, 8));
            b.setCreatedAt(LocalDateTime.now());
        }
        b.setUpdatedAt(LocalDateTime.now());
        bindings.put(b.getId(), b);
        return b;
    }

    public void deleteBinding(String id) {
        bindings.remove(id);
    }

    public List<ExecutionLog> getExecutionLogs() {
        return new ArrayList<>(executionLogs);
    }

    public void addExecutionLog(ExecutionLog log) {
        if (log.getId() == null || log.getId().isEmpty()) {
            log.setId("log-" + UUID.randomUUID().toString().substring(0, 8));
        }
        executionLogs.add(0, log);
        while (executionLogs.size() > 1000) {
            executionLogs.remove(executionLogs.size() - 1);
        }
    }
}
