package com.frs.backend.service;

import com.frs.backend.dto.EmployeeCreateDto;
import com.frs.backend.dto.EmployeeUpdateDto;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface EmployeeService {
    ResponseEntity<Map<String, Object>> all();
    ResponseEntity<Map<String, Object>> create(EmployeeCreateDto body);
    ResponseEntity<Map<String, Object>> update(String id, EmployeeUpdateDto body);
    ResponseEntity<Map<String, Object>> delete(String id);
}
