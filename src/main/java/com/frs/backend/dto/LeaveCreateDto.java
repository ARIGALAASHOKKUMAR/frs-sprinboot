package com.frs.backend.dto;

public record LeaveCreateDto(String leaveType, String fromDate, String toDate, String reason) { }
