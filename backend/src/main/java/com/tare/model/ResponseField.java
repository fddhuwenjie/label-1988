package com.tare.model;

import lombok.Data;

@Data
public class ResponseField {
    private String path; // JSON path like "data.items[0].name"
    private String name;
    private String type; // string, number, boolean, object, array
    private String description;
}
