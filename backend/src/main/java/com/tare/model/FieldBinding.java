package com.tare.model;

import lombok.Data;

@Data
public class FieldBinding {
    private String pushParamName; // Push interface parameter name
    private String sourceFieldPath; // Data source response field path
    private String defaultValue; // Default value if no binding
}
