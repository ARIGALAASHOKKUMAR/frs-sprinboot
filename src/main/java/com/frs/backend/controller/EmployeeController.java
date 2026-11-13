package com.frs.backend.controller;

import com.frs.backend.dto.EmployeeCreateDto;
import com.frs.backend.dto.EmployeeUpdateDto;
import com.frs.backend.service.EmployeeService;
import com.frs.backend.security.AuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService employees;

    public EmployeeController(EmployeeService employees) {
        this.employees = employees;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> all(
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!AuthFilter.admin(request, response)) {
            return ResponseEntity.status(response.getStatus()).build();
        }
        return employees.all();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody EmployeeCreateDto body,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (!AuthFilter.admin(request, response)) {
            return ResponseEntity.status(response.getStatus()).build();
        }
        return employees.create(body);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String id,
            @RequestBody EmployeeUpdateDto body,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (!AuthFilter.admin(request, response)) {
            return ResponseEntity.status(response.getStatus()).build();
        }
        return employees.update(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String id,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (!AuthFilter.admin(request, response)) {
            return ResponseEntity.status(response.getStatus()).build();
        }
        return employees.delete(id);
    }
}
