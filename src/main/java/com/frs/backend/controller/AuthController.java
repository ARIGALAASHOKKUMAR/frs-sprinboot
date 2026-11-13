package com.frs.backend.controller;

import com.frs.backend.dto.LoginRequestDto;
import com.frs.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody(required = false) LoginRequestDto body) {
        return auth.login(body);
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        auth.logout((String) request.getAttribute("sessionToken"));
        return ResponseEntity.ok(Map.of("success", true));
    }
}
