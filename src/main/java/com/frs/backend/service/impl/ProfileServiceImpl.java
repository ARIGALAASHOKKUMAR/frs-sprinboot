package com.frs.backend.service.impl;

import com.frs.backend.repository.EmployeeRepository;
import com.frs.backend.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProfileServiceImpl implements ProfileService {
    private final EmployeeRepository employees;

    public ProfileServiceImpl(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Override
    public ResponseEntity<Map<String, Object>> get(String employeeId) {
        List<Map<String, Object>> rows = employees.findProfile(employeeId);
        if (rows.isEmpty()) {
            return ResponseEntity.status(404).body(
                    Map.of("success", false, "message", "Employee not found."));
        }
        return ResponseEntity.ok(Map.of("success", true, "employee", rows.get(0)));
    }
}
