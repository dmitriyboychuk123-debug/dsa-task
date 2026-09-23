package org.example.consumer.util;

public class ZScoreCalculator {
    private static final double ZERO_STANDARD_DEVIATION = 0.0;
    private static final double ZERO_Z_SCORE = 0.0;

    private ZScoreCalculator() {
    }

    public static double calculateAbsoluteZScore(double value, double mean, double standardDeviation) {
        if (standardDeviation == ZERO_STANDARD_DEVIATION) {
            if (value == mean) {
                return ZERO_Z_SCORE;
            }

            return Double.POSITIVE_INFINITY;
        }

        return Math.abs(value - mean) / standardDeviation;
    }

}
