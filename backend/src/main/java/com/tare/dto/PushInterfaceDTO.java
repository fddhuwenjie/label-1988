package com.tare.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PushInterfaceDTO {
    
    @NotBlank(message = "推送接口名称不能为空")
    @Size(max = 100, message = "推送接口名称不能超过100个字符")
    private String name;
    
    @NotBlank(message = "URL不能为空")
    @Pattern(regexp = "^https?://.*", message = "URL格式不正确，必须以http://或https://开头")
    private String url;
    
    @NotBlank(message = "请求方法不能为空")
    @Pattern(regexp = "^(GET|POST|PUT|DELETE)$", message = "请求方法只能是GET、POST、PUT或DELETE")
    private String method;
    
    private Map<String, String> headers;
    
    @Valid
    private List<RequestParamDTO> requestParams;
    
    private Boolean enabled = true;
    
    @Data
    public static class RequestParamDTO {
        @NotBlank(message = "参数名称不能为空")
        private String name;
        
        @NotBlank(message = "参数类型不能为空")
        @Pattern(regexp = "^(string|number|boolean|object|array)$", message = "参数类型只能是string、number、boolean、object或array")
        private String type;
        
        private String description;
        
        private Boolean required = false;
        
        private String defaultValue;
    }
}
