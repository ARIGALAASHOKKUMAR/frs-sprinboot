package com.frs.backend.repository;

import com.frs.backend.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, String> {
    @Query(value = """
            SELECT id,name,username,mobile,designation,division,district,role,
                   date_of_joining AS "dateOfJoining"
            FROM employees
            WHERE (username=:username OR id=:username) AND password=:password
            """, nativeQuery = true)
    List<Map<String, Object>> findForLogin(
            @Param("username") String username, @Param("password") String password);

    @Query(value = """
            SELECT id,name,username,mobile,designation,division,district,
                   date_of_joining AS "dateOfJoining",created_at AS "createdAt"
            FROM employees
            WHERE role='employee'
            ORDER BY name
            """, nativeQuery = true)
    List<Map<String, Object>> findAllEmployees();

    @Query(value = """
            SELECT id,name,username,mobile,designation,division,district,role,
                   date_of_joining AS "dateOfJoining",created_at AS "createdAt"
            FROM employees
            WHERE id=:id
            """, nativeQuery = true)
    List<Map<String, Object>> findProfile(@Param("id") String id);

    @Query(value = """
            SELECT id,name,username,mobile,designation,division,district,
                   date_of_joining AS "dateOfJoining",created_at AS "createdAt"
            FROM employees
            WHERE id=:id AND role='employee'
            """, nativeQuery = true)
    List<Map<String, Object>> findEmployeeDetailsById(@Param("id") String id);

    Optional<EmployeeEntity> findByIdAndRole(String id, String role);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE employees SET
              id=CASE WHEN :changeId THEN :newId ELSE id END,
              name=CASE WHEN :changeName THEN :newName ELSE name END,
              username=CASE WHEN :changeUsername THEN :newUsername ELSE username END,
              password=CASE WHEN :changePassword THEN :newPassword ELSE password END,
              mobile=CASE WHEN :changeMobile THEN :newMobile ELSE mobile END,
              designation=CASE WHEN :changeDesignation THEN :newDesignation ELSE designation END,
              date_of_joining=CASE WHEN :changeDateOfJoining THEN :newDateOfJoining ELSE date_of_joining END,
              division=CASE WHEN :changeDivision THEN :newDivision ELSE division END,
              district=CASE WHEN :changeDistrict THEN :newDistrict ELSE district END
            WHERE id=:employeeId AND role='employee'
            """, nativeQuery = true)
    int updateEmployee(
            @Param("employeeId") String employeeId,
            @Param("changeId") boolean changeId, @Param("newId") String newId,
            @Param("changeName") boolean changeName, @Param("newName") String newName,
            @Param("changeUsername") boolean changeUsername, @Param("newUsername") String newUsername,
            @Param("changePassword") boolean changePassword, @Param("newPassword") String newPassword,
            @Param("changeMobile") boolean changeMobile, @Param("newMobile") String newMobile,
            @Param("changeDesignation") boolean changeDesignation,
            @Param("newDesignation") String newDesignation,
            @Param("changeDateOfJoining") boolean changeDateOfJoining,
            @Param("newDateOfJoining") LocalDate newDateOfJoining,
            @Param("changeDivision") boolean changeDivision, @Param("newDivision") String newDivision,
            @Param("changeDistrict") boolean changeDistrict, @Param("newDistrict") String newDistrict);
}
