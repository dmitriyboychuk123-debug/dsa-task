package org.example.consumer.util;

import org.slf4j.Logger;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class ZScoreLogger {
    private static final String Z_SCORE_OK_TEMPLATE = "[{}] Data point: {} | Status: OK | Z-score: {}";
    private static final String Z_SCORE_WARM_UP_TEMPLATE = "[{}] Data point: {} | Status: WARMING-UP | Z-score: N/A";
    private static final String Z_SCORE_ANOMALY_TEMPLATE = "[{}] Data point: {} | Status: ANOMALY DETECTED! | Z-score: {} | ALERT: Significant deviation detected.";
    private static final DateTimeFormatter ISO_FIXED_NANOS = new DateTimeFormatterBuilder()
            .appendInstant(9)
            .toFormatter();

    private ZScoreLogger() {
    }

    public static void logInfiniteValue(Logger logger, double newValue, double zScore) {
        logAnomalyValue(logger, newValue, zScore);
    }

    public static void logAnomalyValue(Logger logger, double newValue, double zScore) {
        logger.warn(Z_SCORE_ANOMALY_TEMPLATE, getFixedNanosInstantNow(), formatValue(newValue), formatValue(zScore));
    }

    public static void logWarmUpPhase(Logger logger, double newValue) {
        logger.info(Z_SCORE_WARM_UP_TEMPLATE, getFixedNanosInstantNow(), formatValue(newValue));
    }

    public static void logOkValue(Logger logger, double newValue, double zScore) {
        logger.info(Z_SCORE_OK_TEMPLATE, getFixedNanosInstantNow(), formatValue(newValue), formatValue(zScore));
    }

    public static String getFixedNanosInstantNow() {
        return ISO_FIXED_NANOS.format(Instant.now());
    }

    public static String formatValue(Double value) {
        return String.format(Locale.ROOT,"%.2f", value);
    }
}
