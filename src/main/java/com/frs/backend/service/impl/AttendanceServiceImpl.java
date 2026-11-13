package com.frs.backend.service.impl;

import com.frs.backend.dto.AttendanceLocationDto;
import com.frs.backend.entity.AttendanceEntity;
import com.frs.backend.repository.AttendanceRepository;
import com.frs.backend.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceServiceImpl implements AttendanceService {
    private static final List<Site> SITES = List.of(
            new Site("Office of the Commissioner of Labour", 16.513973, 80.639791),
            new Site("Nidhi Bhavan, Mangalagiri", 16.420984, 80.558304));

    private final AttendanceRepository attendance;

    public AttendanceServiceImpl(AttendanceRepository attendance) {
        this.attendance = attendance;
    }

    @Override
    public ResponseEntity<Map<String, Object>> eligibility(String employeeId) {
        Map<String, Object> previous = attendance.previousDay(employeeId);
        boolean eligible = Boolean.TRUE.equals(previous.get("completeAttendance"))
                || Boolean.TRUE.equals(previous.get("hasRequest"));
        String reason = eligible ? null : Boolean.TRUE.equals(previous.get("hasClockIn"))
                ? "missed_clock_out" : "missed_clock_in";
        String date = previous.get("date").toString();
        String message = eligible ? null : reason.equals("missed_clock_out")
                ? "You clocked in on " + date
                    + " but did not clock out. Submit an attendance regularization to continue."
                : "No clock-in was recorded on " + date
                    + ". Submit a leave request to continue.";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("eligible", eligible);
        body.put("previousDate", date);
        body.put("reason", reason);
        body.put("message", message);
        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<Map<String, Object>> teamStatus() {
        List<Map<String, Object>> rows = attendance.findTeamStatus();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("date", rows.isEmpty() ? null : rows.get(0).get("date"));
        body.put("employees", rows);
        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<Map<String, Object>> history(String employeeId, String from, String to) {
        try {
            if (from != null) {
                LocalDate.parse(from);
            }
            if (to != null) {
                LocalDate.parse(to);
            }
            if (from != null && to != null && from.compareTo(to) > 0) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "Choose a valid date range."));
        }
        return ResponseEntity.ok(Map.of(
                "success", true, "records", attendance.findHistory(employeeId, from, to)));
    }

    @Override
    public ResponseEntity<Map<String, Object>> ownRecords(String employeeId) {
        return ResponseEntity.ok(Map.of(
                "success", true, "records", attendance.findEmployeeRecords(employeeId)));
    }

    @Override
    public ResponseEntity<Map<String, Object>> clockIn(
            String employeeId, AttendanceLocationDto body) {
        LocationResult locationResult = validateLocation(body);
        if (locationResult.error != null) {
            return locationResult.error;
        }
        Location location = locationResult.location;

        List<Map<String, Object>> open = attendance.findOpenToday(employeeId);
        if (!open.isEmpty()) {
            return ResponseEntity.status(409).body(Map.of("success", false,
                    "message", "You already have an active clock-in.", "record", open.get(0)));
        }

        Map<String, Object> previous = attendance.previousDay(employeeId);
        if (!Boolean.TRUE.equals(previous.get("completeAttendance"))
                && !Boolean.TRUE.equals(previous.get("hasRequest"))) {
            String date = previous.get("date").toString();
            String message = Boolean.TRUE.equals(previous.get("hasClockIn"))
                    ? "You clocked in on " + date
                        + " but did not clock out. Submit an attendance regularization to continue."
                    : "No clock-in was recorded on " + date
                        + ". Submit a leave request to continue.";
            return ResponseEntity.status(409).body(Map.of("success", false, "message", message));
        }

        List<Map<String, Object>> today = attendance.findToday(employeeId);
        if (!today.isEmpty()) {
            return ResponseEntity.status(409).body(Map.of("success", false,
                    "message", "Attendance has already been recorded today.", "record", today.get(0)));
        }

        AttendanceEntity newRecord = new AttendanceEntity();
        newRecord.setEmployeeId(employeeId);
        newRecord.setClockIn(Instant.now());
        newRecord.setCreatedAt(LocalDate.now(ZoneId.of("Asia/Kolkata")));
        newRecord.setClockInLatitude(location.latitude);
        newRecord.setClockInLongitude(location.longitude);
        AttendanceEntity savedRecord = attendance.save(newRecord);
        Map<String, Object> record = attendance.findRecord(savedRecord.getId());
        return ResponseEntity.status(201).body(Map.of("success", true, "record", record,
                "site", location.site, "distanceMeters", location.distance));
    }

    @Override
    public ResponseEntity<Map<String, Object>> clockOut(
            String employeeId, AttendanceLocationDto body) {
        LocationResult locationResult = validateLocation(body);
        if (locationResult.error != null) {
            return locationResult.error;
        }
        Location location = locationResult.location;
        int updated = attendance.clockOut(employeeId, location.latitude, location.longitude);
        if (updated == 0) {
            return ResponseEntity.status(409).body(Map.of(
                    "success", false, "message", "No active clock-in was found."));
        }
        List<Map<String, Object>> rows = attendance.findToday(employeeId);
        return ResponseEntity.ok(Map.of("success", true, "record", rows.get(0),
                "site", location.site, "distanceMeters", location.distance));
    }

    private static LocationResult validateLocation(AttendanceLocationDto body) {
        double latitude = number(body.latitude());
        double longitude = number(body.longitude());
        double accuracy = number(body.accuracy());
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            return LocationResult.failure(400, "Enable location and try again to record attendance.");
        }
        if (!Double.isFinite(accuracy) || accuracy < 0 || accuracy > 200) {
            return LocationResult.failure(
                    400, "Your GPS accuracy is too low. Move to an open area and try again.");
        }

        Site nearestSite = SITES.get(0);
        double nearestDistance = distance(latitude, longitude, nearestSite.latitude, nearestSite.longitude);
        for (Site site : SITES) {
            double siteDistance = distance(latitude, longitude, site.latitude, site.longitude);
            if (siteDistance < nearestDistance) {
                nearestSite = site;
                nearestDistance = siteDistance;
            }
        }
        if (nearestDistance + accuracy > 100000) {
            return LocationResult.failure(403,
                    "You must be within 200 m of an approved office to record attendance. " +
                            "Nearest site: " + nearestSite.name + ".");
        }
        return LocationResult.success(new Location(
                latitude, longitude, nearestSite.name, Math.round(nearestDistance)));
    }

    private static double number(Double value) {
        if (value == null) {
            return Double.NaN;
        }
        return value;
    }

    private static double distance(double latitude1, double longitude1,
                                   double latitude2, double longitude2) {
        double radians = Math.PI / 180;
        double latitudeDelta = (latitude2 - latitude1) * radians;
        double longitudeDelta = (longitude2 - longitude1) * radians;
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(latitude1 * radians) * Math.cos(latitude2 * radians)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private record Site(String name, double latitude, double longitude) { }
    private record Location(double latitude, double longitude, String site, long distance) { }
    private record LocationResult(
            Location location, ResponseEntity<Map<String, Object>> error) {
        private static LocationResult success(Location location) {
            return new LocationResult(location, null);
        }

        private static LocationResult failure(int status, String message) {
            return new LocationResult(null,
                    ResponseEntity.status(status).body(
                            Map.of("success", false, "message", message)));
        }
    }
}
