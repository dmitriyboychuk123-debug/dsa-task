package org.example.producer;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.messaging.Message;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "app.features.mock-data-generator.enabled=true",
        "app.features.mock-data-generator.add-anomaly-value=false"
})
class ProducerIntegrationTest {

    // started once per JVM and shared with the cached Spring context; Ryuk removes it when tests finish
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.14.0"))
            .withServices("sqs")
            .withEnv("LOCALSTACK_AUTH_TOKEN", System.getenv().getOrDefault("LOCALSTACK_AUTH_TOKEN", "123"));

    static {
        LOCALSTACK.start();
    }

    @DynamicPropertySource
    static void sqsProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.sqs.endpoint", () -> LOCALSTACK.getEndpoint().toString());
        registry.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", LOCALSTACK::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", LOCALSTACK::getSecretKey);
    }

    @Autowired
    private SqsTemplate sqsTemplate;

    @Value("${sqs.queues.remote-telemetry.name}")
    private String queueName;

    @Value("classpath:/data/normal-distribution.txt")
    private Resource normalDistributionData;

    @Test
    void scheduledTaskPublishesMockDataToQueue() throws IOException {
        List<Double> received = new ArrayList<>();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            sqsTemplate.receiveMany(from -> from.queue(queueName)
                            .maxNumberOfMessages(10)
                            .pollTimeout(Duration.ofSeconds(1)), String.class)
                    .stream()
                    .map(Message::getPayload)
                    .map(Double::valueOf)
                    .forEach(received::add);
            assertThat(received).hasSizeGreaterThanOrEqualTo(20);
        });

        assertThat(received).allSatisfy(value -> assertThat(expectedValues()).contains(value));
    }

    private Set<Double> expectedValues() throws IOException {
        return normalDistributionData.getContentAsString(StandardCharsets.UTF_8)
                .lines()
                .filter(line -> !line.isBlank())
                .map(String::trim)
                .map(Double::valueOf)
                .collect(Collectors.toSet());
    }
}
