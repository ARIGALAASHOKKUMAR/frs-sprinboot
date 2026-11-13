package com.frs.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "employees")
@Data
public class EmployeeEntity {
    @Id
    private String id;
    private String name;
    private String username;
    private String password;
    private String mobile;
    private String designation;
    private String division;
    private String district;
    private String role;
    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public EmployeeEntity() { }
}
