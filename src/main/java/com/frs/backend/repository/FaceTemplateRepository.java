package com.frs.backend.repository;

import com.frs.backend.entity.FaceTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Repository
public interface FaceTemplateRepository extends JpaRepository<FaceTemplateEntity, String> {
    @Query(value = "SELECT EXISTS(SELECT 1 FROM face_templates)", nativeQuery = true)
    boolean existsAny();

    @Query(value = """
            SELECT registered_at AS "registeredAt"
            FROM face_templates
            WHERE employee_id=:employeeId
            """, nativeQuery = true)
    List<Map<String, Object>> findRegisteredAt(@Param("employeeId") String employeeId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO face_templates(employee_id,descriptor,registered_at)
            VALUES ('EMP001',CAST(:descriptor AS DOUBLE PRECISION[]),COALESCE(:registeredAt,NOW()))
            ON CONFLICT(employee_id) DO NOTHING
            """, nativeQuery = true)
    int importLegacy(
            @Param("descriptor") String descriptor,
            @Param("registeredAt") Timestamp registeredAt);
}
