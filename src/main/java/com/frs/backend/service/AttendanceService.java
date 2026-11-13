package com.frs.backend.service;

import com.frs.backend.dto.AttendanceLocationDto;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface AttendanceService {
    ResponseEntity<Map<String, Object>> eligibility(String employeeId);
    ResponseEntity<Map<String, Object>> teamStatus();
    ResponseEntity<Map<String, Object>> history(String employeeId, String from, String to);
    ResponseEntity<Map<String, Object>> ownRecords(String employeeId);
    ResponseEntity<Map<String, Object>> clockIn(String employeeId, AttendanceLocationDto body);
    ResponseEntity<Map<String, Object>> clockOut(String employeeId, AttendanceLocationDto body);
}
