package com.tare.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DataSourceInterface {
    private String id;
    private String name;
    private String url;
    private String method; // GET, POST
    private Map<String, String> headers;
    private Map<String, Object> requestBody;
    private List<ResponseField> responseFields;
    private String postProcessor; // JavaScript code for post-processing
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    private Boolean enabled;
}
