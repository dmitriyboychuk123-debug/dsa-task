package org.example.producer.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.generator")
public record GeneratorProperties(
        double mean,
        @Positive double standardDeviation,
        @DecimalMin("0.0") @DecimalMax("1.0") double anomalyProbability,
        @Positive double anomalyMinSigma,
        @Positive double anomalyMaxSigma
) {
    public GeneratorProperties {
        if (anomalyMinSigma > anomalyMaxSigma) {
            throw new IllegalArgumentException("anomalyMinSigma - " + anomalyMinSigma
                    + " cannot exceed anomalyMaxSigma - " + anomalyMaxSigma);
        }
    }
}
