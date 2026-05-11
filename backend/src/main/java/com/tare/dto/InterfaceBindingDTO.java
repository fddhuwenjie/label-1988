package com.tare.dto;

import com.tare.validator.CronExpressionValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Data
public class InterfaceBindingDTO {
    
    @NotBlank(message = "绑定配置名称不能为空")
    @Size(max = 100, message = "绑定配置名称不能超过100个字符")
    private String name;
    
    @NotBlank(message = "数据源接口ID不能为空")
    private String dataSourceId;
    
    @NotBlank(message = "推送接口ID不能为空")
    private String pushInterfaceId;
    
    @Valid
    private List<FieldBindingDTO> fieldBindings;
    
    @CronExpressionValidator(message = "Cron表达式格式不正确")
    private String cronExpression;
    
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled = true;
    
    @jakarta.validation.constraints.Pattern(regexp = "^(https?://.*)?$", message = "Webhook URL 格式不正确，必须是合法的 http/https 地址")
    private String webhookUrl;
    
    @Min(value = 0, message = "最大重试次数不能小于0")
    @Max(value = 10, message = "最大重试次数不能超过10")
    private Integer maxRetryTimes = 3;
    
    @Data
    public static class FieldBindingDTO {
        @NotBlank(message = "推送参数名称不能为空")
        private String pushParamName;
        
        private String sourceFieldPath;
        
        private String defaultValue;
    }
}
