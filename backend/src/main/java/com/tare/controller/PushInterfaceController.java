package com.tare.controller;

import com.tare.common.Result;
import com.tare.dto.PushInterfaceDTO;
import com.tare.exception.BusinessException;
import com.tare.model.PushInterface;
import com.tare.model.RequestParam;
import com.tare.store.MockDataStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/push-interfaces")
@RequiredArgsConstructor
@Slf4j
public class PushInterfaceController {

    private final MockDataStore dataStore;

    @GetMapping
    public Result<List<PushInterface>> list() {
        log.info("查询推送接口列表");
        List<PushInterface> pushInterfaces = dataStore.getAllPushInterfaces();
        log.debug("查询到{}条推送接口", pushInterfaces.size());
        return Result.success(pushInterfaces);
    }

    @GetMapping("/{id}")
    public Result<PushInterface> get(@PathVariable String id) {
        log.info("查询推送接口: id={}", id);
        PushInterface pushInterface = dataStore.getPushInterface(id);
        if (pushInterface == null) {
            log.warn("推送接口不存在: id={}", id);
            throw BusinessException.notFound("推送接口");
        }
        return Result.success(pushInterface);
    }

    @PostMapping
    public Result<PushInterface> create(@Valid @RequestBody PushInterfaceDTO dto) {
        log.info("创建推送接口: name={}, url={}, method={}", dto.getName(), dto.getUrl(), dto.getMethod());
        
        PushInterface pushInterface = convertToEntity(dto);
        pushInterface.setId(null);
        pushInterface.setCreatedAt(LocalDateTime.now());
        pushInterface.setUpdatedAt(LocalDateTime.now());
        
        PushInterface saved = dataStore.savePushInterface(pushInterface);
        log.info("推送接口创建成功: id={}", saved.getId());
        
        return Result.success(saved);
    }

    @PutMapping("/{id}")
    public Result<PushInterface> update(@PathVariable String id, @Valid @RequestBody PushInterfaceDTO dto) {
        log.info("更新推送接口: id={}, name={}", id, dto.getName());
        
        PushInterface existing = dataStore.getPushInterface(id);
        if (existing == null) {
            log.warn("推送接口不存在: id={}", id);
            throw BusinessException.notFound("推送接口");
        }
        
        PushInterface pushInterface = convertToEntity(dto);
        pushInterface.setId(id);
        pushInterface.setCreatedAt(existing.getCreatedAt());
        pushInterface.setUpdatedAt(LocalDateTime.now());
        
        PushInterface saved = dataStore.savePushInterface(pushInterface);
        log.info("推送接口更新成功: id={}", id);
        
        return Result.success(saved);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        log.info("删除推送接口: id={}", id);
        
        if (dataStore.getPushInterface(id) == null) {
            log.warn("推送接口不存在: id={}", id);
            throw BusinessException.notFound("推送接口");
        }
        
        // 检查是否有绑定配置引用此推送接口
        boolean hasBindings = dataStore.getAllBindings().stream()
                .anyMatch(b -> id.equals(b.getPushInterfaceId()));
        if (hasBindings) {
            log.warn("推送接口被绑定配置引用，无法删除: id={}", id);
            throw BusinessException.invalidParam("该推送接口被绑定配置引用，无法删除");
        }
        
        dataStore.deletePushInterface(id);
        log.info("推送接口删除成功: id={}", id);
        
        return Result.success();
    }
    
    private PushInterface convertToEntity(PushInterfaceDTO dto) {
        PushInterface pushInterface = new PushInterface();
        pushInterface.setName(dto.getName());
        pushInterface.setUrl(dto.getUrl());
        pushInterface.setMethod(dto.getMethod());
        pushInterface.setHeaders(dto.getHeaders());
        pushInterface.setEnabled(dto.getEnabled());
        
        if (dto.getRequestParams() != null) {
            pushInterface.setRequestParams(dto.getRequestParams().stream()
                    .map(rp -> {
                        RequestParam param = new RequestParam();
                        param.setName(rp.getName());
                        param.setType(rp.getType());
                        param.setDescription(rp.getDescription());
                        param.setRequired(rp.getRequired());
                        param.setDefaultValue(rp.getDefaultValue());
                        return param;
                    })
                    .collect(Collectors.toList()));
        }
        
        return pushInterface;
    }
}
