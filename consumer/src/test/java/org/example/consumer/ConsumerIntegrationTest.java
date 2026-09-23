package org.example.consumer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.example.consumer.service.AnomalyDetectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class ConsumerIntegrationTest {

    private static final String MESSAGE_GROUP_ID = "integration-test";

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

    @Value("${app.anomaly-detection.minimum-window-elements}")
    private int minimumWindowElements;

    private final Logger logger = (Logger) LoggerFactory.getLogger(AnomalyDetectionService.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void consumesValuesFromQueueAndDetectsAnomaly() {
        // values 48..52 keep the window's mean at 50 with a small standard deviation
        int normalValuesCount = minimumWindowElements + 10;
        for (int i = 0; i < normalValuesCount; i++) {
            send(50.0 + (i % 5) - 2);
        }
        send(100.0);

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(messages()).hasSize(normalValuesCount + 1));

        List<ILoggingEvent> events = List.copyOf(appender.list);
        assertThat(events.subList(0, minimumWindowElements))
                .allSatisfy(event -> assertThat(event.getFormattedMessage()).contains("Status: WARMING-UP"));
        assertThat(events.subList(minimumWindowElements, normalValuesCount))
                .allSatisfy(event -> assertThat(event.getFormattedMessage()).contains("Status: OK"));

        ILoggingEvent anomaly = events.getLast();
        assertThat(anomaly.getLevel()).isEqualTo(Level.WARN);
        assertThat(anomaly.getFormattedMessage()).contains("Data point: 100.00 | Status: ANOMALY DETECTED!");
    }

    private void send(double value) {
        sqsTemplate.send(to -> to.queue(queueName)
                .messageGroupId(MESSAGE_GROUP_ID)
                .payload(String.valueOf(value)));
    }

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
