package org.example.producer.generator;

import org.example.producer.config.Features;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockDataGeneratorTest {

    private static final Resource NORMAL = resource("1.5\n-2.0\n0.25\n");
    private static final Resource WITH_ANOMALY = resource("10.0\n20.0\n");

    @Test
    void usesNormalDataWhenAnomalyDisabled() {
        MockDataGenerator generator = new MockDataGenerator(features(false), NORMAL, WITH_ANOMALY);

        assertEquals(1.5, generator.generateValue());
        assertEquals(-2.0, generator.generateValue());
        assertEquals(0.25, generator.generateValue());
    }

    @Test
    void usesAnomalyDataWhenAnomalyEnabled() {
        MockDataGenerator generator = new MockDataGenerator(features(true), NORMAL, WITH_ANOMALY);

        assertEquals(10.0, generator.generateValue());
        assertEquals(20.0, generator.generateValue());
    }

    @Test
    void wrapsAroundToFirstValueAfterLastOne() {
        MockDataGenerator generator = new MockDataGenerator(features(true), NORMAL, WITH_ANOMALY);

        generator.generateValue();
        generator.generateValue();

        assertEquals(10.0, generator.generateValue());
    }

    @Test
    void parsesValuesRegardlessOfWhitespace() {
        Resource data = resource("  3.0   4.0\n\n5.0 ");
        MockDataGenerator generator = new MockDataGenerator(features(false), data, WITH_ANOMALY);

        assertEquals(3.0, generator.generateValue());
        assertEquals(4.0, generator.generateValue());
        assertEquals(5.0, generator.generateValue());
    }

    private static Features features(boolean addAnomalyValue) {
        return new Features(new Features.MockDataGenerator(true, addAnomalyValue));
    }

    private static Resource resource(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
    }
}
