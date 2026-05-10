package com.tare.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InterfaceBinding {
    private String id;
    private String name;
    private String dataSourceId;
    private String pushInterfaceId;
    private List<FieldBinding> fieldBindings;
    private String cronExpression; // Cron expression for scheduling
    private Boolean enabled;
    private String webhookUrl;
    private Integer maxRetryCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecutedAt;
}
