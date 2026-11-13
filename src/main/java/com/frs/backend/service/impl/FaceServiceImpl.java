package com.frs.backend.service.impl;

import com.frs.backend.dto.FaceImageDto;
import com.frs.backend.entity.FaceTemplateEntity;
import com.frs.backend.repository.FaceTemplateRepository;
import com.frs.backend.service.FaceDescriptorService;
import com.frs.backend.service.FaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FaceServiceImpl implements FaceService {
    private static final double MATCH_THRESHOLD = 0.4;

    private final FaceTemplateRepository templates;
    private final FaceDescriptorService descriptors;

    public FaceServiceImpl(FaceTemplateRepository templates, FaceDescriptorService descriptors) {
        this.templates = templates;
        this.descriptors = descriptors;
    }

    @Override
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Face Recognition API is running",
                "modelsLoaded", descriptors.loaded(),
                "faceRegistered", templates.existsAny(),
                "endpoints", List.of("POST /api/face/register", "POST /api/face/compare")));
    }

    @Override
    public ResponseEntity<Map<String, Object>> status(String employeeId) {
        List<Map<String, Object>> rows = templates.findRegisteredAt(employeeId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("registered", !rows.isEmpty());
        body.put("registeredAt", rows.isEmpty() ? null : rows.get(0).get("registeredAt"));
        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<Map<String, Object>> register(String employeeId, FaceImageDto body) {
        if (!descriptors.loaded()) {
            return modelUnavailable();
        }
        try {
            double[] descriptor = descriptors.descriptor(Objects.toString(body.image(), null));
            StringBuilder array = new StringBuilder("{");
            for (int i = 0; i < descriptor.length; i++) {
                if (i > 0) {
                    array.append(',');
                }
                array.append(Double.toString(descriptor[i]));
            }
            array.append('}');
            Instant now = Instant.now();
            Double[] storedDescriptor = new Double[descriptor.length];
            for (int i = 0; i < descriptor.length; i++) {
                storedDescriptor[i] = descriptor[i];
            }
            templates.save(new FaceTemplateEntity(employeeId, storedDescriptor, now));
            return ResponseEntity.ok(Map.of("success", true,
                    "message", "Face registered successfully", "registeredAt", now.toString()));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    Objects.toString(exception.getMessage(), "Face registration failed")));
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> compare(String employeeId, FaceImageDto body) {
        if (!descriptors.loaded()) {
            return modelUnavailable();
        }
        try {
            var registered = templates.findById(employeeId);
            if (registered.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "matched", false,
                        "message", "No registered face found. Please register first."));
            }
            Double[] storedDescriptor = registered.get().getDescriptor();
            double[] descriptor = new double[storedDescriptor.length];
            for (int i = 0; i < storedDescriptor.length; i++) {
                descriptor[i] = storedDescriptor[i];
            }
            double distance = FaceDescriptorService.distance(
                    descriptor, descriptors.descriptor(Objects.toString(body.image(), null)));
            boolean matched = distance < MATCH_THRESHOLD;
            return ResponseEntity.ok(Map.of("success", true,
                    "matched", matched,
                    "distance", Math.round(distance * 10000.0) / 10000.0,
                    "threshold", MATCH_THRESHOLD,
                    "message", matched ? "Face matched successfully" : "Face does not match"));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "matched", false,
                    "message", Objects.toString(exception.getMessage(), "Face comparison failed")));
        }
    }

    private static ResponseEntity<Map<String, Object>> modelUnavailable() {
        return ResponseEntity.status(503).body(Map.of(
                "success", false, "message", "Face recognition models are not loaded yet."));
    }
}
