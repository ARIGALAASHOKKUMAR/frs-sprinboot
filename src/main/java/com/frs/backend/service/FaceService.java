package com.frs.backend.service;

import com.frs.backend.dto.FaceImageDto;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface FaceService {
    ResponseEntity<Map<String, Object>> health();
    ResponseEntity<Map<String, Object>> status(String employeeId);
    ResponseEntity<Map<String, Object>> register(String employeeId, FaceImageDto body);
    ResponseEntity<Map<String, Object>> compare(String employeeId, FaceImageDto body);
}
