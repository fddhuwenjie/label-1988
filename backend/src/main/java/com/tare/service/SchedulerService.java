package com.tare.service;

import com.tare.model.InterfaceBinding;
import com.tare.store.MockDataStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final MockDataStore dataStore;
    private final ExecutionService executionService;
    
    private TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, String> taskCronExpressions = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("初始化定时任务调度器");
        
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("binding-scheduler-");
        scheduler.setErrorHandler(t -> log.error("定时任务执行异常", t));
        scheduler.initialize();
        
        this.taskScheduler = scheduler;
        
        // 初始化时加载所有已启用的定时任务
        loadScheduledBindings();
    }
    
    @PreDestroy
    public void destroy() {
        log.info("关闭定时任务调度器");
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduledTasks.clear();
        
        if (taskScheduler instanceof ThreadPoolTaskScheduler) {
            ((ThreadPoolTaskScheduler) taskScheduler).shutdown();
        }
    }
    
    /**
     * 加载所有已启用的定时任务
     */
    private void loadScheduledBindings() {
        log.info("加载已启用的定时任务");
        
        dataStore.getAllBindings().stream()
                .filter(b -> Boolean.TRUE.equals(b.getEnabled()))
                .filter(b -> b.getCronExpression() != null && !b.getCronExpression().isEmpty())
                .forEach(this::scheduleBinding);
    }
    
    /**
     * 每分钟检查绑定配置变更，同步定时任务
     */
    @Scheduled(fixedRate = 60000)
    public void syncScheduledBindings() {
        log.debug("同步定时任务配置");
        
        dataStore.getAllBindings().forEach(binding -> {
            String id = binding.getId();
            String cronExpression = binding.getCronExpression();
            boolean enabled = Boolean.TRUE.equals(binding.getEnabled());
            boolean hasCron = cronExpression != null && !cronExpression.isEmpty();
            
            String currentCron = taskCronExpressions.get(id);
            boolean isScheduled = scheduledTasks.containsKey(id);
            
            // 需要调度：启用 + 有cron + (未调度 或 cron变更)
            if (enabled && hasCron) {
                if (!isScheduled || !cronExpression.equals(currentCron)) {
                    log.info("更新定时任务: bindingId={}, cron={}", id, cronExpression);
                    cancelBinding(id);
                    scheduleBinding(binding);
                }
            } 
            // 需要取消：禁用 或 无cron
            else if (isScheduled) {
                log.info("取消定时任务: bindingId={}", id);
                cancelBinding(id);
            }
        });
        
        // 清理已删除的绑定配置对应的定时任务
        scheduledTasks.keySet().stream()
                .filter(id -> dataStore.getBinding(id) == null)
                .forEach(id -> {
                    log.info("清理已删除绑定的定时任务: bindingId={}", id);
                    cancelBinding(id);
                });
    }
    
    /**
     * 调度绑定配置
     */
    public void scheduleBinding(InterfaceBinding binding) {
        String id = binding.getId();
        String cronExpression = binding.getCronExpression();
        
        if (cronExpression == null || cronExpression.isEmpty()) {
            log.debug("绑定配置无cron表达式，跳过调度: bindingId={}", id);
            return;
        }
        
        try {
            CronTrigger trigger = new CronTrigger(cronExpression);
            
            ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                log.info("定时任务触发: bindingId={}, name={}", id, binding.getName());
                
                // 重新获取最新的绑定配置
                InterfaceBinding currentBinding = dataStore.getBinding(id);
                if (currentBinding == null) {
                    log.warn("绑定配置已删除，跳过执行: bindingId={}", id);
                    return;
                }
                
                if (!Boolean.TRUE.equals(currentBinding.getEnabled())) {
                    log.info("绑定配置已禁用，跳过执行: bindingId={}", id);
                    return;
                }
                
                try {
                    executionService.executeBinding(currentBinding);
                    log.info("定时任务执行成功: bindingId={}", id);
                } catch (Exception e) {
                    log.error("定时任务执行失败: bindingId={}, error={}", id, e.getMessage(), e);
                }
            }, trigger);
            
            scheduledTasks.put(id, future);
            taskCronExpressions.put(id, cronExpression);
            
            log.info("定时任务调度成功: bindingId={}, name={}, cron={}", id, binding.getName(), cronExpression);
            
        } catch (IllegalArgumentException e) {
            log.error("Cron表达式无效: bindingId={}, cron={}, error={}", id, cronExpression, e.getMessage());
        }
    }
    
    /**
     * 取消绑定配置的定时任务
     */
    public void cancelBinding(String bindingId) {
        ScheduledFuture<?> future = scheduledTasks.remove(bindingId);
        taskCronExpressions.remove(bindingId);
        
        if (future != null) {
            future.cancel(false);
            log.info("定时任务已取消: bindingId={}", bindingId);
        }
    }
    
    /**
     * 获取定时任务状态
     */
    public Map<String, TaskStatus> getTaskStatuses() {
        Map<String, TaskStatus> statuses = new ConcurrentHashMap<>();
        
        scheduledTasks.forEach((id, future) -> {
            String cron = taskCronExpressions.get(id);
            boolean cancelled = future.isCancelled();
            boolean done = future.isDone();
            
            statuses.put(id, new TaskStatus(cron, !cancelled && !done, cancelled, done));
        });
        
        return statuses;
    }
    
    public record TaskStatus(String cronExpression, boolean running, boolean cancelled, boolean done) {}
}
