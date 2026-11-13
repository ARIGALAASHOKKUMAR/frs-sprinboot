package com.frs.backend.controller;

import com.frs.backend.dto.LeaveCreateDto;
import com.frs.backend.dto.LeaveStatusDto;
import com.frs.backend.security.AuthFilter;
import com.frs.backend.service.LeaveService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {
    private final LeaveService leaves;

    public LeaveController(LeaveService leaves) {
        this.leaves = leaves;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> all(HttpServletRequest request) {
        boolean admin = "admin".equals(request.getAttribute("role"));
        return leaves.all(admin, (String) request.getAttribute("employeeId"));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody LeaveCreateDto body,
            HttpServletRequest request) {
        return leaves.create(
                (String) request.getAttribute("role"),
                (String) request.getAttribute("employeeId"),
                body);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> status(
            @PathVariable long id,
            @RequestBody LeaveStatusDto body,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (!AuthFilter.admin(request, response)) {
            return ResponseEntity.status(response.getStatus()).build();
        }
        return leaves.updateStatus(id, body.status(), (String) request.getAttribute("employeeId"));
    }
}
