package org.example.consumer.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.anomaly-detection")
public record AnomalyDetectionProperties(
        @Positive double zScoreThreshold,
        @Min(2) int minimumWindowElements,
        @Min(2) int maximumWindowElements,
        @Positive int maximumConsecutiveAnomaliesAllowed
) {
    public AnomalyDetectionProperties {
        if (minimumWindowElements > maximumWindowElements) {
            throw new IllegalArgumentException("minimumWindowElements - " + minimumWindowElements
                    + " cannot exceed maximumWindowElements - " + maximumWindowElements);
        }
    }
}
