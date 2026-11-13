package com.frs.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance")
@Data
public class AttendanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "employee_id")
    private String employeeId;
    @Column(name = "clock_in")
    private Instant clockIn;
    @Column(name = "clock_out")
    private Instant clockOut;
    @Column(name = "created_at")
    private LocalDate createdAt;
    @Column(name = "clock_in_latitude")
    private Double clockInLatitude;
    @Column(name = "clock_in_longitude")
    private Double clockInLongitude;
    @Column(name = "clock_out_latitude")
    private Double clockOutLatitude;
    @Column(name = "clock_out_longitude")
    private Double clockOutLongitude;

    public AttendanceEntity() { }
}
