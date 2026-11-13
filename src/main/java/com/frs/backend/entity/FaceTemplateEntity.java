package com.frs.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "face_templates")
@Data
public class FaceTemplateEntity {
    @Id
    @Column(name = "employee_id")
    private String employeeId;
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "double precision[]")
    private Double[] descriptor;
    @Column(name = "registered_at")
    private Instant registeredAt;

    protected FaceTemplateEntity() { }

    public FaceTemplateEntity(String employeeId, Double[] descriptor, Instant registeredAt) {
        this.employeeId = employeeId;
        this.descriptor = descriptor;
        this.registeredAt = registeredAt;
    }
}
