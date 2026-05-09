package com.tare.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    private final String errorCode;
    
    public BusinessException(String message) {
        super(message);
        this.code = 400;
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public BusinessException(Integer code, String errorCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }
    
    public static BusinessException notFound(String resource) {
        return new BusinessException(404, "NOT_FOUND", resource + "不存在");
    }
    
    public static BusinessException invalidParam(String message) {
        return new BusinessException(400, "INVALID_PARAM", message);
    }
    
    public static BusinessException executionFailed(String message) {
        return new BusinessException(500, "EXECUTION_FAILED", message);
    }
}
