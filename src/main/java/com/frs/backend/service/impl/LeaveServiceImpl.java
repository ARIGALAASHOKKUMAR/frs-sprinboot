package com.frs.backend.service.impl;

import com.frs.backend.dto.LeaveCreateDto;
import com.frs.backend.entity.LeaveRequestEntity;
import com.frs.backend.repository.LeaveRepository;
import com.frs.backend.service.LeaveService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LeaveServiceImpl implements LeaveService {
    private final LeaveRepository leaves;

    public LeaveServiceImpl(LeaveRepository leaves) {
        this.leaves = leaves;
    }

    @Override
    public ResponseEntity<Map<String, Object>> all(boolean admin, String employeeId) {
        return ResponseEntity.ok(Map.of(
                "success", true, "requests", leaves.findRequests(admin, employeeId)));
    }

    @Override
    public ResponseEntity<Map<String, Object>> create(
            String role, String employeeId, LeaveCreateDto body) {
        if (!"employee".equals(role)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false, "message", "Only employees can submit leave requests."));
        }
        String type = Objects.toString(body.leaveType(), "");
        String from = Objects.toString(body.fromDate(), "");
        String to = Objects.toString(body.toDate(), "");
        String reason = Objects.toString(body.reason(), "").trim();
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
            if (type.isBlank() || reason.isBlank() || toDate.isBefore(fromDate)) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    "Enter a leave type, valid date range, and reason."));
        }
        LeaveRequestEntity request = new LeaveRequestEntity();
        request.setEmployeeId(employeeId);
        request.setLeaveType(type);
        request.setFromDate(fromDate);
        request.setToDate(toDate);
        request.setReason(reason);
        request.setStatus("Pending");
        request.setCreatedAt(Instant.now());
        LeaveRequestEntity saved = leaves.save(request);
        Map<String, Object> row = Map.of(
                "id", saved.getId(),
                "leaveType", saved.getLeaveType(),
                "fromDate", saved.getFromDate(),
                "toDate", saved.getToDate(),
                "reason", saved.getReason(),
                "status", saved.getStatus(),
                "createdAt", saved.getCreatedAt());
        return ResponseEntity.status(201).body(Map.of("success", true, "request", row));
    }

    @Override
    public ResponseEntity<Map<String, Object>> updateStatus(long id, String status, String reviewerId) {
        if (!List.of("Approved", "Rejected").contains(status)) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "Status must be Approved or Rejected."));
        }
        int updated = leaves.updateStatus(id, status, reviewerId);
        List<Map<String, Object>> rows = updated == 0 ? List.of() : leaves.findStatus(id);
        if (rows.isEmpty()) {
            return ResponseEntity.status(404).body(
                    Map.of("success", false, "message", "Leave request not found."));
        }
        return ResponseEntity.ok(Map.of("success", true, "request", rows.get(0)));
    }
}
