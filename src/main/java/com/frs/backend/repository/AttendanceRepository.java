package com.frs.backend.repository;

import com.frs.backend.entity.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {
    @Query(value = """
            WITH p AS(SELECT (NOW() AT TIME ZONE 'Asia/Kolkata')::date-1 AS day)
            SELECT TO_CHAR(p.day,'YYYY-MM-DD') AS date,
                   EXISTS(SELECT 1 FROM attendance a WHERE a.employee_id=:employeeId
                     AND (a.clock_in AT TIME ZONE 'Asia/Kolkata')::date=p.day
                     AND a.clock_in IS NOT NULL AND a.clock_out IS NOT NULL) AS "completeAttendance",
                   EXISTS(SELECT 1 FROM attendance a WHERE a.employee_id=:employeeId
                     AND (a.clock_in AT TIME ZONE 'Asia/Kolkata')::date=p.day
                     AND a.clock_in IS NOT NULL) AS "hasClockIn",
                   EXISTS(SELECT 1 FROM leave_requests l WHERE l.employee_id=:employeeId
                     AND l.status IN('Pending','Approved') AND l.from_date<=p.day
                     AND l.to_date>=p.day) AS "hasRequest"
            FROM p
            """, nativeQuery = true)
    Map<String, Object> previousDay(@Param("employeeId") String employeeId);

    @Query(value = """
            WITH today AS(SELECT(NOW() AT TIME ZONE 'Asia/Kolkata')::date AS day_date),
            attendance_today AS(
              SELECT DISTINCT ON(a.employee_id)a.employee_id,
                     a.clock_in AS "clockIn",a.clock_out AS "clockOut"
              FROM attendance a,today t
              WHERE(a.clock_in AT TIME ZONE 'Asia/Kolkata')::date=t.day_date
              ORDER BY a.employee_id,a.clock_in DESC
            ),
            approved_status AS(
              SELECT DISTINCT ON(l.employee_id)l.employee_id,l.leave_type AS "leaveType"
              FROM leave_requests l,today t
              WHERE l.status='Approved' AND l.leave_type NOT ILIKE '%regularization%'
                AND l.from_date<=t.day_date AND l.to_date>=t.day_date
              ORDER BY l.employee_id,l.reviewed_at DESC NULLS LAST,l.created_at DESC
            )
            SELECT e.id AS "employeeId",e.name,a."clockIn",a."clockOut",s."leaveType",
                   CASE WHEN s.employee_id IS NOT NULL AND
                     (s."leaveType" ILIKE '%work from home%' OR s."leaveType" ILIKE '%wfh%')
                     THEN 'WFH' WHEN s.employee_id IS NOT NULL THEN 'Leave'
                     WHEN a.employee_id IS NOT NULL THEN 'In Office'
                     ELSE 'Not Clocked In' END AS status,
                   TO_CHAR(t.day_date,'YYYY-MM-DD') AS date
            FROM employees e CROSS JOIN today t
            LEFT JOIN attendance_today a ON a.employee_id=e.id
            LEFT JOIN approved_status s ON s.employee_id=e.id
            WHERE e.role='employee'
            ORDER BY e.name
            """, nativeQuery = true)
    List<Map<String, Object>> findTeamStatus();

    @Query(value = """
            SELECT a.id,a.employee_id AS "employeeId",e.name,e.designation,
                   a.clock_in AS "clockIn",a.clock_out AS "clockOut",
                   (a.clock_in AT TIME ZONE 'Asia/Kolkata')::date AS date
            FROM attendance a JOIN employees e ON e.id=a.employee_id
            WHERE (:employeeId IS NULL OR :employeeId='' OR a.employee_id=:employeeId)
              AND (:fromDate IS NULL OR
                   (a.clock_in AT TIME ZONE 'Asia/Kolkata')::date>=CAST(:fromDate AS date))
              AND (:toDate IS NULL OR
                   (a.clock_in AT TIME ZONE 'Asia/Kolkata')::date<=CAST(:toDate AS date))
            ORDER BY a.clock_in DESC
            """, nativeQuery = true)
    List<Map<String, Object>> findHistory(
            @Param("employeeId") String employeeId,
            @Param("fromDate") String from,
            @Param("toDate") String to);

    @Query(value = """
            SELECT id,clock_in AS "clockIn",clock_out AS "clockOut",
                   (clock_in AT TIME ZONE 'Asia/Kolkata')::date AS date
            FROM attendance
            WHERE employee_id=:employeeId
            ORDER BY clock_in DESC
            LIMIT 180
            """, nativeQuery = true)
    List<Map<String, Object>> findEmployeeRecords(@Param("employeeId") String employeeId);

    @Query(value = """
            SELECT id,clock_in AS "clockIn",clock_out AS "clockOut",
                   (clock_in AT TIME ZONE 'Asia/Kolkata')::date AS date
            FROM attendance
            WHERE employee_id=:employeeId AND clock_out IS NULL
              AND (clock_in AT TIME ZONE 'Asia/Kolkata')::date=
                  (NOW() AT TIME ZONE 'Asia/Kolkata')::date
            ORDER BY clock_in DESC
            LIMIT 1
            """, nativeQuery = true)
    List<Map<String, Object>> findOpenToday(@Param("employeeId") String employeeId);

    @Query(value = """
            SELECT id,clock_in AS "clockIn",clock_out AS "clockOut",
                   (clock_in AT TIME ZONE 'Asia/Kolkata')::date AS date
            FROM attendance
            WHERE employee_id=:employeeId
              AND (clock_in AT TIME ZONE 'Asia/Kolkata')::date=
                  (NOW() AT TIME ZONE 'Asia/Kolkata')::date
            ORDER BY clock_in DESC
            LIMIT 1
            """, nativeQuery = true)
    List<Map<String, Object>> findToday(@Param("employeeId") String employeeId);

    @Query(value = """
            SELECT id,clock_in AS "clockIn",clock_out AS "clockOut",
                   (clock_in AT TIME ZONE 'Asia/Kolkata')::date AS date
            FROM attendance
            WHERE id=:id
            """, nativeQuery = true)
    Map<String, Object> findRecord(@Param("id") long id);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE attendance
            SET clock_out=NOW(),clock_out_latitude=:latitude,clock_out_longitude=:longitude
            WHERE id=(
              SELECT id FROM attendance
              WHERE employee_id=:employeeId AND clock_out IS NULL
                AND (clock_in AT TIME ZONE 'Asia/Kolkata')::date=
                    (NOW() AT TIME ZONE 'Asia/Kolkata')::date
              ORDER BY clock_in DESC
              LIMIT 1
            )
            """, nativeQuery = true)
    int clockOut(
            @Param("employeeId") String employeeId,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude);
}
