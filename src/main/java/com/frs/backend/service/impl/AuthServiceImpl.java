package com.frs.backend.service.impl;

import com.frs.backend.dto.LoginRequestDto;
import com.frs.backend.entity.AuthSessionEntity;
import com.frs.backend.repository.AuthSessionRepository;
import com.frs.backend.repository.EmployeeRepository;
import com.frs.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AuthServiceImpl implements AuthService {
    private final EmployeeRepository employees;
    private final AuthSessionRepository sessions;
    private final SecureRandom random = new SecureRandom();

    public AuthServiceImpl(EmployeeRepository employees, AuthSessionRepository sessions) {
        this.employees = employees;
        this.sessions = sessions;
    }

    @Override
    public ResponseEntity<Map<String, Object>> login(LoginRequestDto body) {
        String username = body == null ? "" : Objects.toString(body.username(), "").trim();
        String password = body == null ? "" : Objects.toString(body.password(), "");
        List<Map<String, Object>> rows = employees.findForLogin(username, password);
        if (rows.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false, "message", "Invalid username or password."));
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder token = new StringBuilder();
        for (byte value : bytes) {
            token.append(String.format("%02x", value));
        }
        sessions.save(new AuthSessionEntity(
                token.toString(), rows.get(0).get("id").toString(), Instant.now()));
        return ResponseEntity.ok(Map.of(
                "success", true, "token", token.toString(), "employee", rows.get(0)));
    }

    @Override
    public void logout(String token) {
        sessions.deleteById(token);
    }

    @Override
    public List<Map<String, Object>> findPrincipal(String token) {
        return sessions.findPrincipal(token);
    }
}
