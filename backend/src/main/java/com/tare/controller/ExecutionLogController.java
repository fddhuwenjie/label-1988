package com.tare.controller;

import com.tare.common.PageResult;
import com.tare.common.Result;
import com.tare.exception.BusinessException;
import com.tare.model.ExecutionLog;
import com.tare.store.MockDataStore;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/execution-logs")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ExecutionLogController {

    private final MockDataStore dataStore;

    @GetMapping
    public Result<PageResult<ExecutionLog>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
            @RequestParam(required = false) String bindingId,
            @RequestParam(required = false) String status) {
        
        log.info("查询执行日志: current={}, size={}, bindingId={}, status={}", current, size, bindingId, status);
        
        List<ExecutionLog> logs = dataStore.getExecutionLogs();
        
        // Filter
        if (bindingId != null && !bindingId.isEmpty()) {
            logs = logs.stream()
                    .filter(l -> bindingId.equals(l.getBindingId()))
                    .collect(Collectors.toList());
        }
        if (status != null && !status.isEmpty()) {
            logs = logs.stream()
                    .filter(l -> status.equals(l.getStatus()))
                    .collect(Collectors.toList());
        }
        
        long total = logs.size();
        log.debug("过滤后共{}条日志", total);
        
        // Pagination
        int start = (current - 1) * size;
        int end = Math.min(start + size, logs.size());
        
        List<ExecutionLog> pagedLogs = start < logs.size() 
                ? logs.subList(start, end) 
                : List.of();
        
        return Result.success(PageResult.of(pagedLogs, total, current, size));
    }

    @GetMapping("/{id}")
    public Result<ExecutionLog> get(@PathVariable String id) {
        log.info("查询执行日志详情: id={}", id);
        
        return dataStore.getExecutionLogs().stream()
                .filter(l -> id.equals(l.getId()))
                .findFirst()
                .map(l -> {
                    log.debug("找到执行日志: id={}, status={}", l.getId(), l.getStatus());
                    return Result.success(l);
                })
                .orElseThrow(() -> {
                    log.warn("执行日志不存在: id={}", id);
                    return BusinessException.notFound("执行日志");
                });
    }
    
    @GetMapping("/stats")
    public Result<ExecutionStats> getStats() {
        log.info("查询执行统计");
        
        List<ExecutionLog> logs = dataStore.getExecutionLogs();
        
        long total = logs.size();
        long success = logs.stream().filter(l -> "SUCCESS".equals(l.getStatus())).count();
        long failed = logs.stream().filter(l -> "FAILED".equals(l.getStatus())).count();
        double avgDuration = logs.stream()
                .filter(l -> l.getDuration() != null)
                .mapToLong(ExecutionLog::getDuration)
                .average()
                .orElse(0);
        
        ExecutionStats stats = new ExecutionStats(total, success, failed, avgDuration);
        log.debug("执行统计: total={}, success={}, failed={}, avgDuration={}ms", 
                total, success, failed, avgDuration);
        
        return Result.success(stats);
    }
    
    public record ExecutionStats(long total, long success, long failed, double avgDuration) {}
}
