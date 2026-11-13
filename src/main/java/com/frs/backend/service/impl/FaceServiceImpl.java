
package com.frs.backend.service.impl;

import com.frs.backend.dto.FaceImageDto;
import com.frs.backend.entity.FaceTemplateEntity;
import com.frs.backend.repository.FaceTemplateRepository;
import com.frs.backend.service.FaceDescriptorService;
import com.frs.backend.service.FaceService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FaceServiceImpl implements FaceService {

    // OpenCV SFace cosine similarity threshold.
    private static final double MATCH_THRESHOLD = 0.363;

    private final FaceTemplateRepository templates;
    private final FaceDescriptorService descriptors;

    public FaceServiceImpl(
            FaceTemplateRepository templates,
            FaceDescriptorService descriptors
    ) {
        this.templates = templates;
        this.descriptors = descriptors;
    }

    // --------------------------------------------------
    // HEALTH CHECK
    // --------------------------------------------------

    @Override
    public ResponseEntity<Map<String, Object>> health() {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", "UP");
        response.put("modelsLoaded", descriptors.loaded());
        response.put("faceRegistered", templates.existsAny());
        response.put("message", descriptors.loaded()
                ? "Face recognition service is ready"
                : "Face recognition models are not loaded");

        return ResponseEntity.ok(response);
    }

    // --------------------------------------------------
    // FACE REGISTRATION STATUS
    // --------------------------------------------------

    @Override
    public ResponseEntity<Map<String, Object>> status(
            String employeeId
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (employeeId == null || employeeId.isBlank()) {
            response.put("success", false);
            response.put("message", "Employee ID is required");

            return ResponseEntity.badRequest().body(response);
        }

        return templates.findById(employeeId)
                .map(template -> {

                    response.put("success", true);
                    response.put("registered", true);
                    response.put("employeeId", employeeId);
                    response.put("registeredAt",
                            template.getRegisteredAt());

                    response.put("message", "Face already registered");

                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {

                    response.put("success", true);
                    response.put("registered", false);
                    response.put("employeeId", employeeId);
                    response.put("message", "Face not registered");

                    return ResponseEntity.ok(response);
                });
    }

    // --------------------------------------------------
    // REGISTER FACE
    // --------------------------------------------------

    @Override
    public ResponseEntity<Map<String, Object>> register(
            String employeeId,
            FaceImageDto body
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {

            if (employeeId == null || employeeId.isBlank()) {
                return error(
                        HttpStatus.BAD_REQUEST,
                        "Employee ID is required"
                );
            }

            if (body == null || body.image() == null
                    || body.image().isBlank()) {
                return error(
                        HttpStatus.BAD_REQUEST,
                        "Face image is required"
                );
            }

            if (!descriptors.loaded()) {
                return error(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Face recognition models are not loaded"
                );
            }

            // Generate SFace descriptor.
            double[] descriptor = descriptors.descriptor(body.image());

            if (descriptor == null || descriptor.length != 128) {
                return error(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Invalid face descriptor generated"
                );
            }

            // Convert double[] to PostgreSQL Double[].
            Double[] storedDescriptor = Arrays.stream(descriptor)
                    .boxed()
                    .toArray(Double[]::new);

            // Save face template.
            FaceTemplateEntity entity = new FaceTemplateEntity(
                    employeeId,
                    storedDescriptor,
                    Instant.now()
            );

            templates.save(entity);

            response.put("success", true);
            response.put("registered", true);
            response.put("employeeId", employeeId);
            response.put("registeredAt", entity.getRegisteredAt());
            response.put("message", "Face registered successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );

        } catch (Exception e) {

            e.printStackTrace();

            return error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Face registration failed"
            );
        }
    }

    // --------------------------------------------------
    // COMPARE FACE
    // --------------------------------------------------

    @Override
    public ResponseEntity<Map<String, Object>> compare(
            String employeeId,
            FaceImageDto body
    ) {

        try {

            if (employeeId == null || employeeId.isBlank()) {
                return error(
                        HttpStatus.BAD_REQUEST,
                        "Employee ID is required"
                );
            }

            if (body == null || body.image() == null
                    || body.image().isBlank()) {
                return error(
                        HttpStatus.BAD_REQUEST,
                        "Face image is required"
                );
            }

            if (!descriptors.loaded()) {
                return error(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Face recognition models are not loaded"
                );
            }

            // Get registered face.
            FaceTemplateEntity template = templates.findById(employeeId)
                    .orElse(null);

            if (template == null) {
                return error(
                        HttpStatus.NOT_FOUND,
                        "Face not registered for this employee"
                );
            }

            Double[] stored = template.getDescriptor();

            if (stored == null || stored.length != 128) {
                return error(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Stored face descriptor is invalid"
                );
            }

            // Convert stored descriptor.
            double[] storedDescriptor = Arrays.stream(stored)
                    .mapToDouble(Double::doubleValue)
                    .toArray();

            // Generate captured face descriptor.
            double[] capturedDescriptor =
                    descriptors.descriptor(body.image());

            if (capturedDescriptor == null
                    || capturedDescriptor.length != 128) {
                return error(
                        HttpStatus.BAD_REQUEST,
                        "Unable to generate face descriptor"
                );
            }

            // Calculate cosine similarity.
            double similarity =
                    FaceDescriptorService.cosineSimilarity(
                            storedDescriptor,
                            capturedDescriptor
                    );

            boolean matched = similarity >= MATCH_THRESHOLD;

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("success", true);
            response.put("matched", matched);
            response.put("employeeId", employeeId);
            response.put("similarity", similarity);
            response.put("threshold", MATCH_THRESHOLD);

            response.put(
                    "message",
                    matched ? "Face matched" : "Face not match"
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );

        } catch (Exception e) {

            e.printStackTrace();

            return error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Face comparison failed"
            );
        }
    }

    // --------------------------------------------------
    // COMMON ERROR RESPONSE
    // --------------------------------------------------

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status,
            String message
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", false);
        response.put("matched", false);
        response.put("message", message);

        return ResponseEntity.status(status).body(response);
    }
}
