package org.example.consumer.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZScoreLoggerTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "3,         3.00",
            "3.14159,   3.14",
            "2.999,     3.00",
            "-1.5,      -1.50",
            "0,         0.00",
            "1234567.891, 1234567.89"
    })
    void formatsWithTwoDecimalsAndDotSeparator(double value, String expected) {
        assertEquals(expected, ZScoreLogger.formatValue(value));
    }

    @Test
    void formatsSpecialValues() {
        assertEquals("Infinity", ZScoreLogger.formatValue(Double.POSITIVE_INFINITY));
        assertEquals("NaN", ZScoreLogger.formatValue(Double.NaN));
    }

    @Test
    void timestampHasFixedNanosecondPrecision() {
        String timestamp = ZScoreLogger.getFixedNanosInstantNow();

        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{9}Z"), timestamp);
        assertDoesNotThrow(() -> Instant.parse(timestamp));
    }
}
