package com.tare.exception;

import com.tare.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<Map<String, Object>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, errorCode={}, message={}", e.getCode(), e.getErrorCode(), e.getMessage());
        
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("errorCode", e.getErrorCode());
        errorDetail.put("detail", e.getMessage());
        
        Result<Map<String, Object>> result = Result.error(e.getCode(), e.getMessage());
        result.setData(errorDetail);
        return result;
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "校验失败",
                        (existing, replacement) -> existing
                ));
        
        String message = fieldErrors.values().stream().findFirst().orElse("参数校验失败");
        
        log.warn("参数校验失败: {}", fieldErrors);
        
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("errorCode", "VALIDATION_ERROR");
        errorDetail.put("fields", fieldErrors);
        
        Result<Map<String, Object>> result = Result.error(400, message);
        result.setData(errorDetail);
        return result;
    }
    
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, Object>> handleBindException(BindException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "校验失败",
                        (existing, replacement) -> existing
                ));
        
        String message = fieldErrors.values().stream().findFirst().orElse("参数绑定失败");
        
        log.warn("参数绑定失败: {}", fieldErrors);
        
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("errorCode", "BIND_ERROR");
        errorDetail.put("fields", fieldErrors);
        
        Result<Map<String, Object>> result = Result.error(400, message);
        result.setData(errorDetail);
        return result;
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数'%s'类型错误，期望类型: %s", 
                e.getName(), 
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        
        log.warn("参数类型错误: {}", message);
        
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("errorCode", "TYPE_MISMATCH");
        errorDetail.put("parameter", e.getName());
        errorDetail.put("expectedType", e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : null);
        
        Result<Map<String, Object>> result = Result.error(400, message);
        result.setData(errorDetail);
        return result;
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Map<String, Object>> handleException(Exception e) {
        log.error("系统异常", e);
        
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("errorCode", "INTERNAL_ERROR");
        errorDetail.put("detail", "系统内部错误，请稍后重试");
        
        Result<Map<String, Object>> result = Result.error(500, "系统内部错误");
        result.setData(errorDetail);
        return result;
    }
}
