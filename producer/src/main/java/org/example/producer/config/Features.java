package org.example.producer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.features")
public record Features(MockDataGenerator mockDataGenerator) {
    public record MockDataGenerator(
            boolean enabled, boolean addAnomalyValue
    ) {
    }
}
