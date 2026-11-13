package com.frs.backend.repository;

import com.frs.backend.entity.AuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, String> {
    @Query(value = """
            SELECT s.employee_id AS "employeeId",e.role
            FROM auth_sessions s
            JOIN employees e ON e.id=s.employee_id
            WHERE s.token=:token
            """, nativeQuery = true)
    List<Map<String, Object>> findPrincipal(@Param("token") String token);
}
