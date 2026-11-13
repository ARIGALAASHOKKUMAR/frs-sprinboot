package com.frs.backend.repository;

import com.frs.backend.entity.LeaveRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Repository
public interface LeaveRepository extends JpaRepository<LeaveRequestEntity, Long> {
    @Query(value = """
            SELECT l.id,l.employee_id AS "employeeId",e.name AS "employeeName",
                   l.leave_type AS "leaveType",l.from_date AS "fromDate",l.to_date AS "toDate",
                   l.reason,l.status,l.created_at AS "createdAt",l.reviewed_at AS "reviewedAt"
            FROM leave_requests l
            JOIN employees e ON e.id=l.employee_id
            WHERE (:admin=true OR l.employee_id=:employeeId)
            ORDER BY l.created_at DESC
            """, nativeQuery = true)
    List<Map<String, Object>> findRequests(
            @Param("admin") boolean admin, @Param("employeeId") String employeeId);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE leave_requests
            SET status=:status,reviewed_at=NOW(),reviewed_by=:reviewerId
            WHERE id=:id
            """, nativeQuery = true)
    int updateStatus(
            @Param("id") long id,
            @Param("status") String status,
            @Param("reviewerId") String reviewerId);

    @Query(value = """
            SELECT id,status,reviewed_at AS "reviewedAt"
            FROM leave_requests
            WHERE id=:id
            """, nativeQuery = true)
    List<Map<String, Object>> findStatus(@Param("id") long id);
}
