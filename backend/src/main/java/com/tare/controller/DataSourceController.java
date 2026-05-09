package com.tare.controller;

import com.tare.common.Result;
import com.tare.dto.DataSourceDTO;
import com.tare.exception.BusinessException;
import com.tare.model.DataSourceInterface;
import com.tare.model.ResponseField;
import com.tare.store.MockDataStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data-sources")
@RequiredArgsConstructor
@Slf4j
public class DataSourceController {

    private final MockDataStore dataStore;

    @GetMapping
    public Result<List<DataSourceInterface>> list() {
        log.info("查询数据源接口列表");
        List<DataSourceInterface> dataSources = dataStore.getAllDataSourceInterfaces();
        log.debug("查询到{}条数据源接口", dataSources.size());
        return Result.success(dataSources);
    }

    @GetMapping("/{id}")
    public Result<DataSourceInterface> get(@PathVariable String id) {
        log.info("查询数据源接口: id={}", id);
        DataSourceInterface dataSource = dataStore.getDataSourceInterface(id);
        if (dataSource == null) {
            log.warn("数据源接口不存在: id={}", id);
            throw BusinessException.notFound("数据源接口");
        }
        return Result.success(dataSource);
    }

    @PostMapping
    public Result<DataSourceInterface> create(@Valid @RequestBody DataSourceDTO dto) {
        log.info("创建数据源接口: name={}, url={}, method={}", dto.getName(), dto.getUrl(), dto.getMethod());
        
        DataSourceInterface dataSource = convertToEntity(dto);
        dataSource.setId(null);
        dataSource.setCreatedAt(LocalDateTime.now());
        dataSource.setUpdatedAt(LocalDateTime.now());
        
        DataSourceInterface saved = dataStore.saveDataSourceInterface(dataSource);
        log.info("数据源接口创建成功: id={}", saved.getId());
        
        return Result.success(saved);
    }

    @PutMapping("/{id}")
    public Result<DataSourceInterface> update(@PathVariable String id, @Valid @RequestBody DataSourceDTO dto) {
        log.info("更新数据源接口: id={}, name={}", id, dto.getName());
        
        DataSourceInterface existing = dataStore.getDataSourceInterface(id);
        if (existing == null) {
            log.warn("数据源接口不存在: id={}", id);
            throw BusinessException.notFound("数据源接口");
        }
        
        DataSourceInterface dataSource = convertToEntity(dto);
        dataSource.setId(id);
        dataSource.setCreatedAt(existing.getCreatedAt());
        dataSource.setUpdatedAt(LocalDateTime.now());
        
        DataSourceInterface saved = dataStore.saveDataSourceInterface(dataSource);
        log.info("数据源接口更新成功: id={}", id);
        
        return Result.success(saved);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        log.info("删除数据源接口: id={}", id);
        
        if (dataStore.getDataSourceInterface(id) == null) {
            log.warn("数据源接口不存在: id={}", id);
            throw BusinessException.notFound("数据源接口");
        }
        
        // 检查是否有绑定配置引用此数据源
        boolean hasBindings = dataStore.getAllBindings().stream()
                .anyMatch(b -> id.equals(b.getDataSourceId()));
        if (hasBindings) {
            log.warn("数据源接口被绑定配置引用，无法删除: id={}", id);
            throw BusinessException.invalidParam("该数据源接口被绑定配置引用，无法删除");
        }
        
        dataStore.deleteDataSourceInterface(id);
        log.info("数据源接口删除成功: id={}", id);
        
        return Result.success();
    }
    
    private DataSourceInterface convertToEntity(DataSourceDTO dto) {
        DataSourceInterface dataSource = new DataSourceInterface();
        dataSource.setName(dto.getName());
        dataSource.setUrl(dto.getUrl());
        dataSource.setMethod(dto.getMethod());
        dataSource.setHeaders(dto.getHeaders());
        dataSource.setRequestBody(dto.getRequestBody());
        dataSource.setPostProcessor(dto.getPostProcessor());
        dataSource.setEnabled(dto.getEnabled());
        
        if (dto.getResponseFields() != null) {
            dataSource.setResponseFields(dto.getResponseFields().stream()
                    .map(rf -> {
                        ResponseField field = new ResponseField();
                        field.setPath(rf.getPath());
                        field.setName(rf.getName());
                        field.setType(rf.getType());
                        field.setDescription(rf.getDescription());
                        return field;
                    })
                    .collect(Collectors.toList()));
        }
        
        return dataSource;
    }
}
