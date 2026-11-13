package com.frs.backend.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.LinkedHashMap;
import java.util.Map;

public class EmployeeUpdateDto {
    private final Map<String, Object> fields = new LinkedHashMap<>();

    @JsonAnySetter
    public void setField(String name, Object value) {
        fields.put(name, value);
    }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
