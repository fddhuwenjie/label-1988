package com.tare.model;

import lombok.Data;

@Data
public class RequestParam {
    private String name;
    private String type; // string, number, boolean, object, array
    private String description;
    private Boolean required;
    private String defaultValue;
}
