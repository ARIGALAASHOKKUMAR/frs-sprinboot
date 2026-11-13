package com.frs.backend.service;

public interface FaceDescriptorService {

    boolean loaded();

    double[] descriptor(String input) throws Exception;

    static double cosineSimilarity(double[] first, double[] second) {

        if (first.length != second.length) {
            throw new IllegalArgumentException(
                "Descriptors must have equal lengths"
            );
        }

        double dot = 0;
        double normFirst = 0;
        double normSecond = 0;

        for (int i = 0; i < first.length; i++) {
            dot += first[i] * second[i];
            normFirst += first[i] * first[i];
            normSecond += second[i] * second[i];
        }

        if (normFirst == 0 || normSecond == 0) {
            return 0;
        }

        return dot / (Math.sqrt(normFirst) * Math.sqrt(normSecond));
    }
}