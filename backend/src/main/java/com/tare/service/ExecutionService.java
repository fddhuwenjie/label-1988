package com.tare.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.tare.model.*;
import com.tare.store.MockDataStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionService {

    private final MockDataStore dataStore;
    private final PostProcessorService postProcessorService;
    private final RetryService retryService;
    private final RestTemplate restTemplate = new RestTemplate();

    public void executeBinding(InterfaceBinding binding) {
        doExecuteBinding(binding, 0);
    }

    public void executeBindingWithRetryCount(InterfaceBinding binding, int retryCount) {
        doExecuteBinding(binding, retryCount);
    }

    private void doExecuteBinding(InterfaceBinding binding, int retryCount) {
        boolean mdcInjected = false;
        if (MDC.get("traceId") == null) {
            MDC.put("traceId", "exec-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            mdcInjected = true;
        }

        log.info("开始执行绑定配置: id={}, name={}, retryCount={}", binding.getId(), binding.getName(), retryCount);

        ExecutionLog executionLog = new ExecutionLog();
        executionLog.setBindingId(binding.getId());
        executionLog.setBindingName(binding.getName());
        executionLog.setRetryCount(retryCount);
        executionLog.setExecutedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        
        long startTime = System.currentTimeMillis();
        String errorMessage = null;
        
        try {
            DataSourceInterface dataSource = dataStore.getDataSourceInterface(binding.getDataSourceId());
            PushInterface pushInterface = dataStore.getPushInterface(binding.getPushInterfaceId());
            
            if (dataSource == null) {
                throw new RuntimeException("数据源接口不存在: " + binding.getDataSourceId());
            }
            if (pushInterface == null) {
                throw new RuntimeException("推送接口不存在: " + binding.getPushInterfaceId());
            }
            
            log.debug("数据源: id={}, name={}", dataSource.getId(), dataSource.getName());
            log.debug("推送接口: id={}, name={}", pushInterface.getId(), pushInterface.getName());
            
            executionLog.setDataSourceId(dataSource.getId());
            executionLog.setDataSourceName(dataSource.getName());
            executionLog.setPushInterfaceId(pushInterface.getId());
            executionLog.setPushInterfaceName(pushInterface.getName());
            
            String dataSourceRequest = buildDataSourceRequest(dataSource);
            executionLog.setDataSourceRequest(dataSourceRequest);
            
            ResponseEntity<String> dataSourceEntity = callDataSource(dataSource);
            String dataSourceResponse = dataSourceEntity.getBody();
            executionLog.setDataSourceStatus(dataSourceEntity.getStatusCode().value());
            
            String processedResponse = dataSourceResponse;
            if (dataSource.getPostProcessor() != null && !dataSource.getPostProcessor().isEmpty()) {
                log.info("执行数据后置处理: dataSourceId={}", dataSource.getId());
                processedResponse = postProcessorService.process(dataSourceResponse, dataSource.getPostProcessor());
            }
            
            executionLog.setDataSourceResponse(processedResponse);
            
            Map<String, Object> pushRequestData = buildPushRequest(binding, processedResponse, pushInterface);
            String pushRequest = JSON.toJSONString(pushRequestData);
            executionLog.setPushRequest(pushRequest);
            
            ResponseEntity<String> pushEntity = callPushInterface(pushInterface, pushRequestData);
            executionLog.setPushResponse(pushEntity.getBody());
            executionLog.setPushStatus(pushEntity.getStatusCode().value());
            
            executionLog.setStatus("SUCCESS");
            log.info("绑定配置执行成功: id={}, duration={}ms", binding.getId(), System.currentTimeMillis() - startTime);
            
            binding.setLastExecutedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            dataStore.saveBinding(binding);
            
        } catch (Exception e) {
            log.error("绑定配置执行失败: id={}, retryCount={}, error={}", binding.getId(), retryCount, e.getMessage(), e);
            errorMessage = e.getMessage();
            executionLog.setStatus("FAILED");
            executionLog.setErrorMessage(errorMessage);
            if (e instanceof RestClientResponseException ex) {
                int statusCode = ex.getStatusCode().value();
                if (executionLog.getDataSourceStatus() == null) {
                    executionLog.setDataSourceStatus(statusCode);
                } else {
                    executionLog.setPushStatus(statusCode);
                }
            }
        } finally {
            executionLog.setDuration(System.currentTimeMillis() - startTime);
            dataStore.addExecutionLog(executionLog);
            if (mdcInjected) {
                MDC.remove("traceId");
            }
        }

        if (errorMessage != null && retryCount == 0) {
            retryService.scheduleRetry(binding, errorMessage, 0);
        }
    }

    private String buildDataSourceRequest(DataSourceInterface dataSource) {
        Map<String, Object> request = new HashMap<>();
        request.put("url", dataSource.getUrl());
        request.put("method", dataSource.getMethod());
        request.put("headers", dataSource.getHeaders());
        if (dataSource.getRequestBody() != null && !dataSource.getRequestBody().isEmpty()) {
            request.put("body", dataSource.getRequestBody());
        }
        return JSON.toJSONString(request);
    }

    private ResponseEntity<String> callDataSource(DataSourceInterface dataSource) {
        // 离线可验证：example.com 使用内置 Mock 响应，不发起真实网络请求
        if (isExampleComUrl(dataSource.getUrl())) {
            String mockBody = getMockDataSourceResponse(dataSource.getUrl());
            ResponseEntity<String> mockResponse = ResponseEntity.ok(mockBody);
            log.info("数据源接口使用本地 Mock 响应: url={}, status={} (无网络调用)", dataSource.getUrl(), mockResponse.getStatusCode().value());
            return mockResponse;
        }

        HttpHeaders headers = new HttpHeaders();
        if (dataSource.getHeaders() != null) {
            dataSource.getHeaders().forEach(headers::set);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> entity;
        if ("POST".equalsIgnoreCase(dataSource.getMethod()) && dataSource.getRequestBody() != null) {
            entity = new HttpEntity<>(dataSource.getRequestBody(), headers);
        } else {
            entity = new HttpEntity<>(headers);
        }

        ResponseEntity<String> response = restTemplate.exchange(
                dataSource.getUrl(),
                HttpMethod.valueOf(dataSource.getMethod().toUpperCase()),
                entity,
                String.class
        );

        log.info("数据源接口调用成功: url={}, status={}", dataSource.getUrl(), response.getStatusCode());
        return response;
    }

    private ResponseEntity<String> callPushInterface(PushInterface pushInterface, Map<String, Object> requestData) {
        // 离线可验证：example.com 使用内置 Mock 响应，不发起真实网络请求
        if (isExampleComUrl(pushInterface.getUrl())) {
            String mockBody = "{\"code\":200,\"message\":\"success (mock)\"}";
            ResponseEntity<String> mockResponse = ResponseEntity.ok(mockBody);
            log.info("推送接口使用本地 Mock 响应: url={}, status={} (无网络调用)", pushInterface.getUrl(), mockResponse.getStatusCode().value());
            return mockResponse;
        }

        HttpHeaders headers = new HttpHeaders();
        if (pushInterface.getHeaders() != null) {
            pushInterface.getHeaders().forEach(headers::set);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                pushInterface.getUrl(),
                HttpMethod.valueOf(pushInterface.getMethod().toUpperCase()),
                entity,
                String.class
        );

        log.info("推送接口调用成功: url={}, status={}", pushInterface.getUrl(), response.getStatusCode());
        return response;
    }

    /** 判断是否为 example.com 域名，用于离线 Mock */
    private boolean isExampleComUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        return url.contains("example.com");
    }

    /** 为 example.com 数据源返回内置 Mock 响应，与 MockDataStore 示例数据结构一致 */
    private String getMockDataSourceResponse(String url) {
        if (url == null) return "{}";
        if (url.contains("/users") || url.contains("/user")) {
            return "{\"code\":200,\"data\":{\"id\":\"user-123\",\"name\":\"张三\",\"email\":\"zhangsan@example.com\"}}";
        }
        if (url.contains("/orders") || url.contains("/order")) {
            return "{\"code\":200,\"data\":{\"orders\":[{\"id\":\"order-001\",\"status\":\"shipped\"},{\"id\":\"order-002\",\"status\":\"pending\"}]}}";
        }
        return "{\"code\":200,\"data\":{}}";
    }

    private Map<String, Object> buildPushRequest(InterfaceBinding binding, String dataSourceResponse, PushInterface pushInterface) {
        Map<String, Object> result = new HashMap<>();
        JSONObject responseJson = JSON.parseObject(dataSourceResponse);
        
        if (binding.getFieldBindings() == null || binding.getFieldBindings().isEmpty()) {
            log.warn("绑定配置无字段映射: bindingId={}", binding.getId());
            return result;
        }
        
        for (FieldBinding fieldBinding : binding.getFieldBindings()) {
            String paramName = fieldBinding.getPushParamName();
            String sourcePath = fieldBinding.getSourceFieldPath();
            String defaultValue = fieldBinding.getDefaultValue();
            
            Object value = null;
            if (sourcePath != null && !sourcePath.isEmpty()) {
                try {
                    String jsonPath = sourcePath.startsWith("$") ? sourcePath : "$." + sourcePath;
                    value = JSONPath.eval(responseJson, jsonPath);
                } catch (Exception e) {
                    log.warn("字段提取失败: path={}, error={}", sourcePath, e.getMessage());
                }
            }
            
            if (value == null && defaultValue != null && !defaultValue.isEmpty()) {
                value = defaultValue;
            }
            
            if (value != null) {
                result.put(paramName, value);
            }
        }
        
        return result;
    }
}
