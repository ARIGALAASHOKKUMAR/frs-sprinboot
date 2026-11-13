package com.frs.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "auth_sessions")
@Data
public class AuthSessionEntity {
    @Id
    private String token;
    @Column(name = "employee_id")
    private String employeeId;
    @Column(name = "created_at")
    private Instant createdAt;

    protected AuthSessionEntity() { }

    public AuthSessionEntity(String token, String employeeId, Instant createdAt) {
        this.token = token;
        this.employeeId = employeeId;
        this.createdAt = createdAt;
    }
}
