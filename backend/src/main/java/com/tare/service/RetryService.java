package com.tare.service;

import com.alibaba.fastjson2.JSON;
import com.tare.model.ExecutionLog;
import com.tare.model.InterfaceBinding;
import com.tare.store.MockDataStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {

    private final ExecutionService executionService;
    private final MockDataStore dataStore;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final int[] RETRY_INTERVALS = {5, 15, 45};

    @Async
    public void scheduleRetry(InterfaceBinding binding, String errorMessage, int currentRetry) {
        int maxRetry = binding.getMaxRetryCount() == null ? 3 : binding.getMaxRetryCount();
        if (currentRetry > maxRetry) {
            log.warn("已达到最大重试次数，触发告警: bindingId={}, retryCount={}", binding.getId(), currentRetry - 1);
            sendAlert(binding, errorMessage, currentRetry - 1);
            return;
        }

        int intervalIndex = Math.min(currentRetry - 1, RETRY_INTERVALS.length - 1);
        long delaySeconds = RETRY_INTERVALS[intervalIndex];

        log.info("安排第{}次重试: bindingId={}, 延迟{}秒", currentRetry, binding.getId(), delaySeconds);

        try {
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("重试等待被中断: bindingId={}", binding.getId(), e);
            return;
        }

        executeRetry(binding, currentRetry);
    }

    private void executeRetry(InterfaceBinding binding, int retryCount) {
        log.info("开始第{}次重试执行: bindingId={}, name={}", retryCount, binding.getId(), binding.getName());

        ExecutionLog executionLog = new ExecutionLog();
        executionLog.setBindingId(binding.getId());
        executionLog.setBindingName(binding.getName());
        executionLog.setExecutedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        executionLog.setRetryCount(retryCount);

        long startTime = System.currentTimeMillis();

        try {
            executionService.executeBindingInternal(binding, executionLog);
            executionLog.setStatus("SUCCESS");
            log.info("第{}次重试执行成功: bindingId={}, duration={}ms", retryCount, binding.getId(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("第{}次重试执行失败: bindingId={}, error={}", retryCount, binding.getId(), e.getMessage(), e);
            executionLog.setStatus("FAILED");
            executionLog.setErrorMessage(e.getMessage());
            executionLog.setDuration(System.currentTimeMillis() - startTime);
            dataStore.addExecutionLog(executionLog);

            scheduleRetry(binding, e.getMessage(), retryCount + 1);
            return;
        }

        executionLog.setDuration(System.currentTimeMillis() - startTime);
        dataStore.addExecutionLog(executionLog);
    }

    private void sendAlert(InterfaceBinding binding, String errorMessage, int retryCount) {
        String webhookUrl = binding.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.info("未配置Webhook URL，跳过告警: bindingId={}", binding.getId());
            return;
        }

        log.info("发送告警通知: bindingId={}, webhookUrl={}", binding.getId(), webhookUrl);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("bindingName", binding.getName());
            payload.put("bindingId", binding.getId());
            payload.put("failureReason", errorMessage);
            payload.put("failureTime", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            payload.put("retryCount", retryCount);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(payload), headers);

            restTemplate.postForEntity(webhookUrl, entity, String.class);
            log.info("告警通知发送成功: bindingId={}", binding.getId());
        } catch (Exception e) {
            log.error("告警通知发送失败: bindingId={}, error={}", binding.getId(), e.getMessage(), e);
        }
    }
}
