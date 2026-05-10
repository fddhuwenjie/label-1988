package com.tare.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExecutionLog {
    private String id;
    private String bindingId;
    private String bindingName;
    private String dataSourceId;
    private String dataSourceName;
    private String pushInterfaceId;
    private String pushInterfaceName;
    private String dataSourceRequest;
    private String dataSourceResponse;
    private Integer dataSourceStatus;
    private String pushRequest;
    private String pushResponse;
    private Integer pushStatus;
    private String status; // SUCCESS, FAILED, PARTIAL
    private String errorMessage;
    private Integer retryCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedAt;
    private Long duration; // milliseconds
}
