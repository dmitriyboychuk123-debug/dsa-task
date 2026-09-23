package org.example.producer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sqs.queues")
public record SqsProperties(
        SqsConfig remoteTelemetry
) {
    public record SqsConfig(
            String name
    ) {
    }
}
