package com.frs.backend.controller;

import com.frs.backend.dto.FaceImageDto;
import com.frs.backend.service.FaceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FaceController {
    private final FaceService faces;

    public FaceController(FaceService faces) {
        this.faces = faces;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> health() {
        return faces.health();
    }

    @GetMapping("/api/face/status")
    public ResponseEntity<Map<String, Object>> status(HttpServletRequest request) {
        return faces.status((String) request.getAttribute("employeeId"));
    }

    @PostMapping("/api/face/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody FaceImageDto body,
            HttpServletRequest request) {
        return faces.register((String) request.getAttribute("employeeId"), body);
    }

    @PostMapping("/api/face/compare")
    public ResponseEntity<Map<String, Object>> compare(
            @RequestBody FaceImageDto body,
            HttpServletRequest request) {
        return faces.compare((String) request.getAttribute("employeeId"), body);
    }
}
