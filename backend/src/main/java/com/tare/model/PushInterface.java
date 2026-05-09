package com.tare.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PushInterface {
    private String id;
    private String name;
    private String url;
    private String method; // GET, POST, PUT, DELETE
    private Map<String, String> headers;
    private List<RequestParam> requestParams;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    private Boolean enabled;
}
