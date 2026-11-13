package com.frs.backend.dto;

public record EmployeeCreateDto(
        String id,
        String name,
        String username,
        String password,
        String mobile,
        String designation,
        String dateOfJoining,
        String division,
        String district) { }
