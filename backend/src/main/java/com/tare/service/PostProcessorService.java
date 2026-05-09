package com.tare.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

/**
 * 数据后置处理服务。
 * <p>
 * JavaScript 引擎可用性说明：Java 15+ 已移除 Nashorn，本项目通过 org.openjdk.nashorn 依赖提供。
 * 若 Nashorn/graaljs 均不可用，会降级到内置声明式指令（extract:、map:）。
 * <p>
 * 若需更强 JS 能力，建议：① 集成 GraalVM JavaScript（graal-js）；② 或将复杂逻辑改为声明式规则。
 */
@Service
@Slf4j
public class PostProcessorService {

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    /**
     * 执行后置处理脚本
     *
     * @param responseData         原始响应数据
     * @param postProcessorScript  JavaScript 后置处理脚本或内置指令
     * @return 处理后的数据
     */
    public String process(String responseData, String postProcessorScript) {
        if (postProcessorScript == null || postProcessorScript.trim().isEmpty()) {
            log.debug("无后置处理脚本，返回原始数据");
            return responseData;
        }
        
        log.info("开始执行后置处理脚本");
        log.debug("原始数据: {}", responseData);
        log.debug("处理脚本: {}", postProcessorScript);
        
        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName("nashorn");
            if (engine == null) {
                engine = scriptEngineManager.getEngineByName("javascript");
            }
            
            if (engine == null) {
                log.warn("JavaScript引擎不可用，使用内置处理器");
                return processWithBuiltIn(responseData, postProcessorScript);
            }
            
            // 将响应数据注入脚本环境
            engine.put("data", JSON.parseObject(responseData));
            engine.put("rawData", responseData);
            
            // 包装脚本，确保返回处理后的数据
            String wrappedScript = String.format(
                    "(function() { var input = data; %s; return typeof result !== 'undefined' ? result : input; })()",
                    postProcessorScript
            );
            
            Object result = engine.eval(wrappedScript);
            String processedData = JSON.toJSONString(result);
            
            log.info("后置处理完成");
            log.debug("处理后数据: {}", processedData);
            
            return processedData;
            
        } catch (ScriptException e) {
            log.error("后置处理脚本执行失败: {}", e.getMessage());
            throw new RuntimeException("后置处理脚本执行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 内置处理器（当 JavaScript 引擎不可用时的兜底方案）
     */
    private String processWithBuiltIn(String responseData, String postProcessorScript) {
        JSONObject data = JSON.parseObject(responseData);
        
        // 解析简单的处理指令
        String[] instructions = postProcessorScript.split(";");
        for (String instruction : instructions) {
            instruction = instruction.trim();
            if (instruction.isEmpty()) continue;
            
            // 支持简单的字段提取: extract:path
            if (instruction.startsWith("extract:")) {
                String path = instruction.substring(8).trim();
                Object extracted = extractPath(data, path);
                if (extracted != null) {
                    return JSON.toJSONString(extracted);
                }
            }
            
            // 支持字段过滤: filter:field=value
            if (instruction.startsWith("filter:")) {
                String condition = instruction.substring(7).trim();
                data = applyFilter(data, condition);
            }
            
            // 支持字段映射: map:oldField->newField
            if (instruction.startsWith("map:")) {
                String mapping = instruction.substring(4).trim();
                data = applyMapping(data, mapping);
            }
        }
        
        return data.toJSONString();
    }
    
    private Object extractPath(JSONObject data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        
        for (String part : parts) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).get(part);
            } else if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    private JSONObject applyFilter(JSONObject data, String condition) {
        // 简单实现，实际可扩展
        return data;
    }
    
    private JSONObject applyMapping(JSONObject data, String mapping) {
        String[] parts = mapping.split("->");
        if (parts.length == 2) {
            String oldField = parts[0].trim();
            String newField = parts[1].trim();
            
            if (data.containsKey(oldField)) {
                Object value = data.remove(oldField);
                data.put(newField, value);
            }
        }
        return data;
    }
}
