package com.frs.backend.service;

import com.frs.backend.dto.LeaveCreateDto;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface LeaveService {
    ResponseEntity<Map<String, Object>> all(boolean admin, String employeeId);
    ResponseEntity<Map<String, Object>> create(String role, String employeeId, LeaveCreateDto body);
    ResponseEntity<Map<String, Object>> updateStatus(long id, String status, String reviewerId);
}
