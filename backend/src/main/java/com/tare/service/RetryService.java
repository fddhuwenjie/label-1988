package com.tare.service;

import com.tare.model.InterfaceBinding;
import com.tare.store.MockDataStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class RetryService {

    private final MockDataStore dataStore;
    private final ExecutionService executionService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final long[] RETRY_DELAYS_SECONDS = {5, 15, 45};

    public RetryService(MockDataStore dataStore, @Lazy ExecutionService executionService) {
        this.dataStore = dataStore;
        this.executionService = executionService;
    }

    @Async
    public void scheduleRetry(InterfaceBinding binding, String errorMessage, int currentRetryCount) {
        int maxRetry = binding.getMaxRetryCount() != null ? binding.getMaxRetryCount() : 3;
        if (currentRetryCount >= maxRetry) {
            log.warn("绑定配置已达到最大重试次数，触发告警通知: bindingId={}, retryCount={}", binding.getId(), currentRetryCount);
            sendWebhookAlert(binding, errorMessage, currentRetryCount);
            return;
        }

        int delayIndex = Math.min(currentRetryCount, RETRY_DELAYS_SECONDS.length - 1);
        long delaySeconds = RETRY_DELAYS_SECONDS[delayIndex];

        log.info("计划重试绑定配置: bindingId={}, retryCount={}, delaySeconds={}", binding.getId(), currentRetryCount + 1, delaySeconds);

        try {
            Thread.sleep(delaySeconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("重试等待被中断: bindingId={}", binding.getId());
            return;
        }

        InterfaceBinding latestBinding = dataStore.getBinding(binding.getId());
        if (latestBinding == null) {
            log.warn("绑定配置已删除，取消重试: bindingId={}", binding.getId());
            return;
        }

        if (!Boolean.TRUE.equals(latestBinding.getEnabled())) {
            log.info("绑定配置已禁用，取消重试: bindingId={}", latestBinding.getId());
            return;
        }

        int nextRetryCount = currentRetryCount + 1;
        log.info("开始第{}次重试绑定配置: bindingId={}", nextRetryCount, latestBinding.getId());

        try {
            executionService.executeBindingWithRetryCount(latestBinding, nextRetryCount);
            log.info("重试执行成功: bindingId={}, retryCount={}", latestBinding.getId(), nextRetryCount);
        } catch (Exception e) {
            log.error("重试执行失败: bindingId={}, retryCount={}, error={}", latestBinding.getId(), nextRetryCount, e.getMessage());
            scheduleRetry(latestBinding, e.getMessage(), nextRetryCount);
        }
    }

    private void sendWebhookAlert(InterfaceBinding binding, String errorMessage, int retryCount) {
        String webhookUrl = binding.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.info("未配置 Webhook URL，跳过告警通知: bindingId={}", binding.getId());
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("bindingName", binding.getName());
            payload.put("errorMessage", errorMessage);
            payload.put("failedAt", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
            payload.put("retryCount", retryCount);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(webhookUrl, entity, String.class);
            log.info("Webhook 告警通知发送成功: bindingId={}, webhookUrl={}", binding.getId(), webhookUrl);
        } catch (Exception e) {
            log.error("Webhook 告警通知发送失败: bindingId={}, webhookUrl={}, error={}", binding.getId(), webhookUrl, e.getMessage());
        }
    }
}
