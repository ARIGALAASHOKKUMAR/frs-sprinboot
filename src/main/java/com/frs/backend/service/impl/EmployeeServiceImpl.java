package com.frs.backend.service.impl;

import com.frs.backend.dto.EmployeeCreateDto;
import com.frs.backend.dto.EmployeeUpdateDto;
import com.frs.backend.entity.EmployeeEntity;
import com.frs.backend.repository.EmployeeRepository;
import com.frs.backend.service.EmployeeService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    private static final Set<String> FIELDS = Set.of(
            "id", "name", "username", "password", "mobile",
            "designation", "dateOfJoining", "division", "district");

    private final EmployeeRepository employees;

    public EmployeeServiceImpl(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Override
    public ResponseEntity<Map<String, Object>> all() {
        return ResponseEntity.ok(Map.of("success", true, "employees", employees.findAllEmployees()));
    }

    @Override
    public ResponseEntity<Map<String, Object>> create(EmployeeCreateDto body) {
        String id = trim(body.id());
        String name = trim(body.name());
        String username = trim(body.username());
        String password = Objects.toString(body.password(), "");
        String designation = trim(body.designation());
        String date = body.dateOfJoining();
        if (id.isEmpty() || name.isEmpty() || username.isEmpty() || password.isEmpty()
                || designation.isEmpty() || date == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    "Employee ID, name, username, password, designation, and date of joining are required."));
        }
        try {
            EmployeeEntity employee = new EmployeeEntity();
            employee.setId(id);
            employee.setName(name);
            employee.setUsername(username);
            employee.setPassword(password);
            employee.setMobile((String) nullable(body.mobile()));
            employee.setDesignation(designation);
            employee.setDateOfJoining(LocalDate.parse(date));
            employee.setDivision((String) defaultValue(body.division(), "Guntur Division"));
            employee.setDistrict((String) defaultValue(body.district(), "Guntur"));
            employee.setRole("employee");
            employee.setCreatedAt(OffsetDateTime.now());
            employees.save(employee);
            Map<String, Object> row = employees.findEmployeeDetailsById(id).get(0);
            return ResponseEntity.status(201).body(Map.of("success", true, "employee", row));
        } catch (DataIntegrityViolationException exception) {
            return duplicateEmployee();
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> update(String id, EmployeeUpdateDto body) {
        Map<String, Object> updates = body.toMap();
        Map<String, Object> values = new LinkedHashMap<>();
        FIELDS.forEach(key -> {
            if (updates.containsKey(key)) {
                Object value = updates.get(key);
                if (!key.equals("password")) {
                    value = key.equals("dateOfJoining")
                            ? (nullable(value) == null ? null : LocalDate.parse(value.toString()))
                            : nullable(value);
                }
                values.put(key, value);
            }
        });
        if (values.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "No employee fields were provided."));
        }
        try {
            Object dateValue = values.get("dateOfJoining");
            int updated = employees.updateEmployee(
                    id,
                    values.containsKey("id"), (String) values.get("id"),
                    values.containsKey("name"), (String) values.get("name"),
                    values.containsKey("username"), (String) values.get("username"),
                    values.containsKey("password"), Objects.toString(values.get("password"), null),
                    values.containsKey("mobile"), (String) values.get("mobile"),
                    values.containsKey("designation"), (String) values.get("designation"),
                    values.containsKey("dateOfJoining"),
                    dateValue == null ? null : LocalDate.parse(dateValue.toString()),
                    values.containsKey("division"), (String) values.get("division"),
                    values.containsKey("district"), (String) values.get("district"));
            if (updated == 0) {
                return ResponseEntity.status(404).body(
                        Map.of("success", false, "message", "Employee not found."));
            }
            String updatedId = values.containsKey("id") ? (String) values.get("id") : id;
            Map<String, Object> employee = employees.findEmployeeDetailsById(updatedId).get(0);
            return ResponseEntity.ok(Map.of("success", true, "employee", employee));
        } catch (DataIntegrityViolationException exception) {
            return duplicateEmployee();
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> delete(String id) {
        var employee = employees.findByIdAndRole(id, "employee");
        if (employee.isEmpty()) {
            return ResponseEntity.status(404).body(
                    Map.of("success", false, "message", "Employee not found."));
        }
        employees.delete(employee.get());
        return ResponseEntity.ok(Map.of("success", true, "deletedId", id));
    }

    private static ResponseEntity<Map<String, Object>> duplicateEmployee() {
        return ResponseEntity.status(409).body(
                Map.of("success", false, "message", "Employee ID or username is already in use."));
    }

    private static String trim(Object value) {
        return Objects.toString(value, "").trim();
    }

    private static Object nullable(Object value) {
        String result = trim(value);
        return result.isEmpty() ? null : result;
    }

    private static Object defaultValue(Object value, String fallback) {
        Object normalized = nullable(value);
        return normalized == null ? fallback : normalized;
    }
}
