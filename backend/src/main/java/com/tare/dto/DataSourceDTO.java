package com.tare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DataSourceDTO {
    
    @NotBlank(message = "数据源名称不能为空")
    @Size(max = 100, message = "数据源名称不能超过100个字符")
    private String name;
    
    @NotBlank(message = "URL不能为空")
    @Pattern(regexp = "^https?://.*", message = "URL格式不正确，必须以http://或https://开头")
    private String url;
    
    @NotBlank(message = "请求方法不能为空")
    @Pattern(regexp = "^(GET|POST)$", message = "请求方法只能是GET或POST")
    private String method;
    
    private Map<String, String> headers;
    
    private Map<String, Object> requestBody;
    
    private List<ResponseFieldDTO> responseFields;
    
    private String postProcessor;
    
    private Boolean enabled = true;
    
    @Data
    public static class ResponseFieldDTO {
        @NotBlank(message = "字段路径不能为空")
        private String path;
        
        @NotBlank(message = "字段名称不能为空")
        private String name;
        
        @NotBlank(message = "字段类型不能为空")
        @Pattern(regexp = "^(string|number|boolean|object|array)$", message = "字段类型只能是string、number、boolean、object或array")
        private String type;
        
        private String description;
    }
}
