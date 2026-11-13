package com.frs.backend.service;

public interface FaceDescriptorService {
    boolean loaded();
    double[] descriptor(String input) throws Exception;

    static double distance(double[] first, double[] second) {
        if (first.length != second.length) {
            throw new IllegalArgumentException("Descriptors must have equal lengths");
        }
        double sum = 0;
        for (int i = 0; i < first.length; i++) {
            double difference = first[i] - second[i];
            sum += difference * difference;
        }
        return Math.sqrt(sum);
    }
}
