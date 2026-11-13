package com.frs.backend.service.impl;

import com.frs.backend.service.FaceDescriptorService;

import jakarta.annotation.PostConstruct;

import nu.pattern.OpenCV;

import org.opencv.core.*;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Service
public class FaceServiceImpl implements FaceDescriptorService {

    private FaceDetectorYN detector;
    private FaceRecognizerSF recognizer;

    private volatile boolean loaded = false;

    @PostConstruct
    public void load() {

        try {

            OpenCV.loadLocally();

            String detectorPath = extractModel(
                "/models/face_detection_yunet_2023mar.onnx"
            );

            String recognizerPath = extractModel(
                "/models/face_recognition_sface_2021dec.onnx"
            );

            detector = FaceDetectorYN.create(
                detectorPath,
                "",
                new Size(320, 320),
                0.9f,
                0.3f,
                5000
            );

            recognizer = FaceRecognizerSF.create(
                recognizerPath,
                ""
            );

            loaded = detector != null && recognizer != null;

            System.out.println(
                "Face recognition models loaded: " + loaded
            );

        } catch (Exception e) {

            loaded = false;

            System.err.println(
                "Face model initialization failed: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private String extractModel(String resourcePath) throws IOException {

        try (InputStream input =
                 getClass().getResourceAsStream(resourcePath)) {

            if (input == null) {
                throw new IOException(
                    "Model not found: " + resourcePath
                );
            }

            String fileName = resourcePath.substring(
                resourcePath.lastIndexOf('/') + 1
            );

            Path tempFile = Files.createTempFile(
                "frs-", "-" + fileName
            );

            Files.copy(
                input,
                tempFile,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            tempFile.toFile().deleteOnExit();

            return tempFile.toAbsolutePath().toString();
        }
    }

    @Override
    public boolean loaded() {
        return loaded;
    }

    @Override
    public double[] descriptor(String input) throws Exception {

        if (!loaded) {
            throw new IllegalStateException(
                "Face recognition models are not loaded"
            );
        }

        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(
                "Face image is required"
            );
        }

        String base64 = input.replaceFirst(
            "^data:image/[A-Za-z0-9.+-]+;base64,",
            ""
        );

        byte[] imageBytes;

        try {
            imageBytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid image data");
        }

        if (imageBytes.length == 0) {
            throw new IllegalArgumentException("Invalid image data");
        }

        Mat image = org.opencv.imgcodecs.Imgcodecs.imdecode(
            new MatOfByte(imageBytes),
            org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR
        );

        if (image.empty()) {
            throw new IllegalArgumentException(
                "Unable to read face image"
            );
        }

        try {

            detector.setInputSize(
                new Size(image.cols(), image.rows())
            );

            Mat faces = new Mat();

            detector.detect(image, faces);

            if (faces.empty() || faces.rows() == 0) {
                throw new IllegalArgumentException(
                    "No face detected. Please capture a clear face image."
                );
            }

            // Select the face with the highest detection confidence.
            int bestIndex = 0;
            double bestConfidence = -1;

            for (int i = 0; i < faces.rows(); i++) {

                double[] values = faces.get(i, 0);

                if (values != null && values.length >= 15) {

                    double confidence = values[14];

                    if (confidence > bestConfidence) {
                        bestConfidence = confidence;
                        bestIndex = i;
                    }
                }
            }

            Mat face = faces.row(bestIndex);

            Mat alignedFace = new Mat();

            recognizer.alignCrop(
                image,
                face,
                alignedFace
            );

            Mat feature = new Mat();

            recognizer.feature(
                alignedFace,
                feature
            );

            if (feature.empty()) {
                throw new IllegalStateException(
                    "Unable to generate face descriptor"
                );
            }

            int total = (int) (feature.total() * feature.channels());

            double[] descriptor = new double[total];

            float[] featureValues = new float[total];

            feature.get(0, 0, featureValues);

            for (int i = 0; i < total; i++) {
                descriptor[i] = featureValues[i];
            }

            if (descriptor.length != 128) {
                throw new IllegalStateException(
                    "Unexpected face descriptor size: " +
                    descriptor.length
                );
            }

            return descriptor;

        } finally {
            image.release();
        }
    }
}