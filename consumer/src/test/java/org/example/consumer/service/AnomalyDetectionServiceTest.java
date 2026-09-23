package org.example.consumer.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.example.consumer.config.AnomalyDetectionProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnomalyDetectionServiceTest {

    private static final double Z_SCORE_THRESHOLD = 3.0;
    private static final int MIN_WINDOW = 3;
    private static final int MAX_WINDOW = 5;
    private static final int MAX_CONSECUTIVE_ANOMALIES = 2;

    private final Logger logger = (Logger) LoggerFactory.getLogger(AnomalyDetectionService.class);
    private ListAppender<ILoggingEvent> appender;
    private AnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        service = newService(MIN_WINDOW, MAX_WINDOW);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void valuesBeforeMinimumWindowAreWarmUp() {
        register(10, 12, 14);

        List<ILoggingEvent> events = appender.list;
        assertEquals(3, events.size());
        events.forEach(event -> {
            assertEquals(Level.INFO, event.getLevel());
            assertTrue(message(event).contains("Status: WARMING-UP | Z-score: N/A"), message(event));
        });
    }

    @Test
    void normalValueIsReportedAsOk() {
        register(10, 12, 14); // mean 12, sample sd 2

        service.registerValue(13.0);

        assertEquals(Level.INFO, last().getLevel());
        assertTrue(message(last()).contains("Data point: 13.00 | Status: OK | Z-score: 0.50"), message(last()));
    }

    @Test
    void valueAboveThresholdIsReportedAsAnomaly() {
        register(10, 12, 14);

        service.registerValue(30.0); // z = 9

        assertEquals(Level.WARN, last().getLevel());
        assertTrue(message(last()).contains("Data point: 30.00 | Status: ANOMALY DETECTED! | Z-score: 9.00"), message(last()));
    }

    @Test
    void valueExactlyAtThresholdIsNotAnomaly() {
        register(10, 12, 14);

        service.registerValue(18.0); // z = 3.0, threshold is exclusive

        assertTrue(message(last()).contains("Status: OK | Z-score: 3.00"), message(last()));
    }

    @Test
    void anomalyIsNotAddedToWindow() {
        register(10, 12, 14);

        register(30, 13);

        assertTrue(message(last()).contains("Z-score: 0.50"), message(last()));
    }

    @Test
    void anomalyIsAddedToWindowAfterMaxConsecutiveAnomalies() {
        register(10, 12, 14);

        register(30, 30, 30); // first two are skipped, third is added to window
        service.registerValue(13.0); // window [10, 12, 14, 30]: mean 16.5, sd ~9.147

        assertTrue(message(last()).contains("Status: OK | Z-score: 0.38"), message(last()));
    }

    @Test
    void normalValueResetsConsecutiveAnomalyCounter() {
        register(10, 12, 14);

        register(30, 13, 30, 30); // 13 resets the counter and joins the window; both later anomalies are skipped
        service.registerValue(13.0); // window [10, 12, 14, 13]: mean 12.25, sd ~1.708

        assertTrue(message(last()).contains("Z-score: 0.44"), message(last()));
    }

    @Test
    void differentValueAfterConstantWindowIsInfiniteAnomaly() {
        register(5, 5, 5);

        service.registerValue(6.0);

        assertEquals(Level.WARN, last().getLevel());
        assertTrue(message(last()).contains("Status: ANOMALY DETECTED! | Z-score: Infinity"), message(last()));
    }

    @Test
    void sameValueAfterConstantWindowIsOk() {
        register(5, 5, 5);

        service.registerValue(5.0);

        assertTrue(message(last()).contains("Status: OK | Z-score: 0.00"), message(last()));
    }

    @Test
    void oldestValueIsEvictedFromRollingWindow() {
        service = newService(3, 3);
        register(100, 10, 12, 14); // 14 is OK and pushes 100 out -> window [10, 12, 14]

        service.registerValue(13.0);

        assertTrue(message(last()).contains("Status: OK | Z-score: 0.50"), message(last()));
    }

    private AnomalyDetectionService newService(int minWindow, int maxWindow) {
        return new AnomalyDetectionService(new AnomalyDetectionProperties(
                Z_SCORE_THRESHOLD, minWindow, maxWindow, MAX_CONSECUTIVE_ANOMALIES));
    }

    private void register(double... values) {
        for (double value : values) {
            service.registerValue(value);
        }
    }

    private ILoggingEvent last() {
        return appender.list.getLast();
    }

    private static String message(ILoggingEvent event) {
        return event.getFormattedMessage();
    }
}
