package org.example.consumer.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZScoreCalculatorTest {

    private static final double DELTA = 1e-9;

    @ParameterizedTest(name = "value={0}, mean={1}, sd={2} -> {3}")
    @CsvSource({
            "12,    10,   2,    1.0",   // above mean
            "8,     10,   2,    1.0",   // below mean -> absolute value
            "10,    10,   2,    0.0",   // equal to mean
            "16,    10,   2,    3.0",
            "-5,    -10,  5,    1.0",   // negative values
            "0,     0,    1,    0.0",
            "10.5,  10,   0.25, 2.0",   // std dev below 1 amplifies the score
            "1000,  0,    1,    1000.0"
    })
    void calculatesAbsoluteZScore(double value, double mean, double standardDeviation, double expected) {
        assertEquals(expected, ZScoreCalculator.calculateAbsoluteZScore(value, mean, standardDeviation), DELTA);
    }

    @Test
    void zeroStandardDeviationAndValueEqualToMeanGivesZero() {
        assertEquals(0.0, ZScoreCalculator.calculateAbsoluteZScore(5, 5, 0), DELTA);
    }

    @ParameterizedTest(name = "value {0}")
    @ValueSource(doubles = {5.0000001, 4.9999999, -5, 1e9})
    void zeroStandardDeviationAndDifferentValueGivesPositiveInfinity(double value) {
        assertEquals(Double.POSITIVE_INFINITY, ZScoreCalculator.calculateAbsoluteZScore(value, 5, 0));
    }

    @Test
    void negativeZeroStandardDeviationIsTreatedAsZero() {
        assertEquals(Double.POSITIVE_INFINITY, ZScoreCalculator.calculateAbsoluteZScore(6, 5, -0.0));
    }

    @Test
    void resultIsNeverNegative() {
        assertEquals(4.0, ZScoreCalculator.calculateAbsoluteZScore(-30, 10, 10), DELTA);
    }
}
