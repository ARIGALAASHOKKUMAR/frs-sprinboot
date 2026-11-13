package com.frs.backend.service;

import com.frs.backend.dto.LoginRequestDto;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface AuthService {
    ResponseEntity<Map<String, Object>> login(LoginRequestDto body);
    void logout(String token);
    List<Map<String, Object>> findPrincipal(String token);
}
