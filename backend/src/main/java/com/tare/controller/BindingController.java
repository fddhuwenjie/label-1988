package com.tare.controller;

import com.tare.common.Result;
import com.tare.dto.InterfaceBindingDTO;
import com.tare.exception.BusinessException;
import com.tare.model.FieldBinding;
import com.tare.model.InterfaceBinding;
import com.tare.service.ExecutionService;
import com.tare.store.MockDataStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bindings")
@RequiredArgsConstructor
@Slf4j
public class BindingController {

    private final MockDataStore dataStore;
    private final ExecutionService executionService;

    @GetMapping
    public Result<List<InterfaceBinding>> list() {
        log.info("查询绑定配置列表");
        List<InterfaceBinding> bindings = dataStore.getAllBindings();
        log.debug("查询到{}条绑定配置", bindings.size());
        return Result.success(bindings);
    }

    @GetMapping("/{id}")
    public Result<InterfaceBinding> get(@PathVariable String id) {
        log.info("查询绑定配置: id={}", id);
        InterfaceBinding binding = dataStore.getBinding(id);
        if (binding == null) {
            log.warn("绑定配置不存在: id={}", id);
            throw BusinessException.notFound("绑定配置");
        }
        return Result.success(binding);
    }

    @PostMapping
    public Result<InterfaceBinding> create(@Valid @RequestBody InterfaceBindingDTO dto) {
        log.info("创建绑定配置: name={}, dataSourceId={}, pushInterfaceId={}", 
                dto.getName(), dto.getDataSourceId(), dto.getPushInterfaceId());
        
        // 验证数据源和推送接口是否存在
        validateReferences(dto.getDataSourceId(), dto.getPushInterfaceId());
        
        InterfaceBinding binding = convertToEntity(dto);
        binding.setId(null);
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());
        
        InterfaceBinding saved = dataStore.saveBinding(binding);
        log.info("绑定配置创建成功: id={}", saved.getId());
        
        return Result.success(saved);
    }

    @PutMapping("/{id}")
    public Result<InterfaceBinding> update(@PathVariable String id, @Valid @RequestBody InterfaceBindingDTO dto) {
        log.info("更新绑定配置: id={}, name={}", id, dto.getName());
        
        InterfaceBinding existing = dataStore.getBinding(id);
        if (existing == null) {
            log.warn("绑定配置不存在: id={}", id);
            throw BusinessException.notFound("绑定配置");
        }
        
        // 验证数据源和推送接口是否存在
        validateReferences(dto.getDataSourceId(), dto.getPushInterfaceId());
        
        InterfaceBinding binding = convertToEntity(dto);
        binding.setId(id);
        binding.setCreatedAt(existing.getCreatedAt());
        binding.setLastExecutedAt(existing.getLastExecutedAt());
        binding.setUpdatedAt(LocalDateTime.now());
        
        InterfaceBinding saved = dataStore.saveBinding(binding);
        log.info("绑定配置更新成功: id={}", id);
        
        return Result.success(saved);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        log.info("删除绑定配置: id={}", id);
        
        if (dataStore.getBinding(id) == null) {
            log.warn("绑定配置不存在: id={}", id);
            throw BusinessException.notFound("绑定配置");
        }
        
        dataStore.deleteBinding(id);
        log.info("绑定配置删除成功: id={}", id);
        
        return Result.success();
    }

    @PostMapping("/{id}/execute")
    public Result<String> execute(@PathVariable String id) {
        log.info("手动执行绑定配置: id={}", id);
        
        InterfaceBinding binding = dataStore.getBinding(id);
        if (binding == null) {
            log.warn("绑定配置不存在: id={}", id);
            throw BusinessException.notFound("绑定配置");
        }
        
        try {
            executionService.executeBinding(binding);
            log.info("绑定配置执行成功: id={}", id);
            return Result.success("执行成功");
        } catch (Exception e) {
            log.error("绑定配置执行失败: id={}, error={}", id, e.getMessage(), e);
            throw BusinessException.executionFailed(e.getMessage());
        }
    }
    
    private void validateReferences(String dataSourceId, String pushInterfaceId) {
        if (dataStore.getDataSourceInterface(dataSourceId) == null) {
            log.warn("数据源接口不存在: id={}", dataSourceId);
            throw BusinessException.invalidParam("数据源接口不存在: " + dataSourceId);
        }
        if (dataStore.getPushInterface(pushInterfaceId) == null) {
            log.warn("推送接口不存在: id={}", pushInterfaceId);
            throw BusinessException.invalidParam("推送接口不存在: " + pushInterfaceId);
        }
    }
    
    private InterfaceBinding convertToEntity(InterfaceBindingDTO dto) {
        InterfaceBinding binding = new InterfaceBinding();
        binding.setName(dto.getName());
        binding.setDataSourceId(dto.getDataSourceId());
        binding.setPushInterfaceId(dto.getPushInterfaceId());
        binding.setCronExpression(dto.getCronExpression());
        binding.setEnabled(dto.getEnabled());
        binding.setWebhookUrl(dto.getWebhookUrl());
        binding.setMaxRetryTimes(dto.getMaxRetryTimes());
        
        if (dto.getFieldBindings() != null) {
            binding.setFieldBindings(dto.getFieldBindings().stream()
                    .map(fb -> {
                        FieldBinding fieldBinding = new FieldBinding();
                        fieldBinding.setPushParamName(fb.getPushParamName());
                        fieldBinding.setSourceFieldPath(fb.getSourceFieldPath());
                        fieldBinding.setDefaultValue(fb.getDefaultValue());
                        return fieldBinding;
                    })
                    .collect(Collectors.toList()));
        }
        
        return binding;
    }
}
