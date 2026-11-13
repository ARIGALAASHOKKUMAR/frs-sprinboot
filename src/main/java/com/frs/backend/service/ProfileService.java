package com.frs.backend.service;

import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface ProfileService {
    ResponseEntity<Map<String, Object>> get(String employeeId);
}
