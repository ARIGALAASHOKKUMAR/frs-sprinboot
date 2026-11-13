package com.frs.backend.controller;

import com.frs.backend.dto.AttendanceLocationDto;
import com.frs.backend.security.AuthFilter;
import com.frs.backend.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService attendance;

    public AttendanceController(AttendanceService attendance) {
        this.attendance = attendance;
    }

    @GetMapping("/clock-in-eligibility")
    public ResponseEntity<Map<String, Object>> eligibility(HttpServletRequest request) {
        return attendance.eligibility(employeeId(request));
    }

    @GetMapping("/team-status")
    public ResponseEntity<Map<String, Object>> team(
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!AuthFilter.admin(request, response)) {
            return ResponseEntity.status(response.getStatus()).build();
        }
        return attendance.teamStatus();
    }

    @GetMapping("/admin-history")
    public ResponseEntity<Map<String, Object>> history(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (!AuthFilter.admin(request, response)) {
            return ResponseEntity.status(response.getStatus()).build();
        }
        return attendance.history(employeeId, from, to);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> own(HttpServletRequest request) {
        return attendance.ownRecords(employeeId(request));
    }

    @PostMapping("/clock-in")
    public ResponseEntity<Map<String, Object>> clockIn(
            @RequestBody AttendanceLocationDto body,
            HttpServletRequest request) {
        return attendance.clockIn(employeeId(request), body);
    }

    @PostMapping("/clock-out")
    public ResponseEntity<Map<String, Object>> clockOut(
            @RequestBody AttendanceLocationDto body,
            HttpServletRequest request) {
        return attendance.clockOut(employeeId(request), body);
    }

    private static String employeeId(HttpServletRequest request) {
        return AuthFilter.employee(request);
    }

}
