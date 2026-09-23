package org.example.consumer.service;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.example.consumer.config.AnomalyDetectionProperties;
import org.example.consumer.util.ZScoreCalculator;
import org.example.consumer.util.ZScoreLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnomalyDetectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnomalyDetectionService.class);
    private final AnomalyDetectionProperties properties;
    private final DescriptiveStatistics rollingWindow;

    private final Object lock = new Object();
    private int consecutiveAnomalyNumber = 0;

    public AnomalyDetectionService(AnomalyDetectionProperties properties) {
        this.properties = properties;
        rollingWindow = new DescriptiveStatistics(properties.maximumWindowElements());
    }

    public void registerValue(Double value) {
        double zScore;
        boolean isAnomaly;
        synchronized (lock) {
            zScore = calculateZScore(value);
            isAnomaly = isAnomaly(zScore);

            if (isAnomaly) {
                if (consecutiveAnomalyNumber < properties.maximumConsecutiveAnomaliesAllowed()) {
                    consecutiveAnomalyNumber++;
                } else {
                    rollingWindow.addValue(value);
                }
            } else {
                consecutiveAnomalyNumber = 0;
                rollingWindow.addValue(value);
            }
        }

        logZScore(value, zScore, isAnomaly);
    }

    private double calculateZScore(Double value) {
        if (rollingWindow.getN() < properties.minimumWindowElements()) {
            return Double.NaN;
        }

        double mean = rollingWindow.getMean();
        double standardDeviation = rollingWindow.getStandardDeviation();
        return ZScoreCalculator.calculateAbsoluteZScore(value, mean, standardDeviation);
    }

    private boolean isAnomaly(double zScore) {
        if (Double.isNaN(zScore)) {
            return false;
        }
        return zScore > properties.zScoreThreshold();
    }

    private void logZScore(double newValue, double zScore, boolean isAnomaly) {
        if (Double.isInfinite(zScore)) {
            ZScoreLogger.logInfiniteValue(LOGGER, newValue, zScore);
        } else {
            if (isAnomaly) {
                ZScoreLogger.logAnomalyValue(LOGGER, newValue, zScore);
            } else {
                if (Double.isNaN(zScore)) {
                    ZScoreLogger.logWarmUpPhase(LOGGER, newValue);
                } else {
                    ZScoreLogger.logOkValue(LOGGER, newValue, zScore);
                }
            }
        }
    }
}
