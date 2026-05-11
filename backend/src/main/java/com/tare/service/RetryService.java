package com.tare.service;

import com.alibaba.fastjson2.JSON;
import com.tare.model.ExecutionLog;
import com.tare.model.InterfaceBinding;
import com.tare.store.MockDataStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {

    private final MockDataStore dataStore;
    private final ExecutionService executionService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final int[] RETRY_INTERVALS = {5, 15, 45};

    @Async
    public void scheduleRetry(InterfaceBinding binding, int retryCount, String lastErrorMessage) {
        int maxRetryTimes = binding.getMaxRetryTimes() != null ? binding.getMaxRetryTimes() : 3;
        
        if (retryCount >= maxRetryTimes) {
            log.warn("已达到最大重试次数 {}，触发告警通知: bindingId={}", maxRetryTimes, binding.getId());
            triggerAlert(binding, lastErrorMessage, retryCount);
            return;
        }

        long delaySeconds = RETRY_INTERVALS[retryCount];
        log.info("调度第 {} 次重试，延迟 {} 秒: bindingId={}", retryCount + 1, delaySeconds, binding.getId());

        try {
            TimeUnit.SECONDS.sleep(delaySeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("重试等待被中断: bindingId={}", binding.getId(), e);
            return;
        }

        executeRetry(binding, retryCount);
    }

    private void executeRetry(InterfaceBinding binding, int retryCount) {
        log.info("开始第 {} 次重试执行: bindingId={}, name={}", retryCount + 1, binding.getId(), binding.getName());

        ExecutionLog executionLog = new ExecutionLog();
        executionLog.setBindingId(binding.getId());
        executionLog.setBindingName(binding.getName());
        executionLog.setExecutedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        executionLog.setRetryTimes(retryCount + 1);

        long startTime = System.currentTimeMillis();

        try {
            executionService.executeBindingInternal(binding, executionLog);
            
            log.info("第 {} 次重试执行成功: bindingId={}, duration={}ms", retryCount + 1, binding.getId(), System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("第 {} 次重试执行失败: bindingId={}, error={}", retryCount + 1, binding.getId(), e.getMessage(), e);
            executionLog.setStatus("FAILED");
            executionLog.setErrorMessage(e.getMessage());
            executionLog.setDuration(System.currentTimeMillis() - startTime);
            dataStore.addExecutionLog(executionLog);

            scheduleRetry(binding, retryCount + 1, e.getMessage());
            return;
        }

        executionLog.setDuration(System.currentTimeMillis() - startTime);
        dataStore.addExecutionLog(executionLog);
    }

    private void triggerAlert(InterfaceBinding binding, String errorMessage, int retryCount) {
        String webhookUrl = binding.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.info("未配置 Webhook URL，跳过告警通知: bindingId={}", binding.getId());
            return;
        }

        try {
            Map<String, Object> alertPayload = new HashMap<>();
            alertPayload.put("bindingName", binding.getName());
            alertPayload.put("bindingId", binding.getId());
            alertPayload.put("failureReason", errorMessage);
            alertPayload.put("failureTime", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            alertPayload.put("retryTimes", retryCount);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(alertPayload), headers);

            log.info("发送告警通知到 Webhook: url={}, bindingId={}", webhookUrl, binding.getId());
            ResponseEntity<String> response = restTemplate.exchange(webhookUrl, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("告警通知发送成功: bindingId={}, status={}", binding.getId(), response.getStatusCode().value());
            } else {
                log.warn("告警通知发送异常: bindingId={}, status={}", binding.getId(), response.getStatusCode().value());
            }
        } catch (Exception e) {
            log.error("告警通知发送失败: bindingId={}, error={}", binding.getId(), e.getMessage(), e);
        }
    }
}
