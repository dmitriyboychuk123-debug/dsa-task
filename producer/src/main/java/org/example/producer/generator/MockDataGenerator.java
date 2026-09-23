package org.example.producer.generator;

import org.example.producer.config.Features;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(name = "app.features.mock-data-generator.enabled",
        havingValue = "true")
public class MockDataGenerator implements DataGenerator {
    private final AtomicInteger indexPointer = new AtomicInteger(0);
    private final List<Double> mockData;

    public MockDataGenerator(Features features,
                             @Value("classpath:/data/normal-distribution.txt") Resource normalDistributionData,
                             @Value("classpath:/data/normal-distribution-with-anomaly.txt") Resource normalDistributionDataWithAnomaly) {

        Resource resource = features.mockDataGenerator().addAnomalyValue() ? normalDistributionDataWithAnomaly : normalDistributionData;
        this.mockData = init(resource);
    }

    private static List<Double> init(Resource resource) {
        try (Scanner scanner = new Scanner(resource.getInputStream(), StandardCharsets.UTF_8)) {
            scanner.useLocale(Locale.ROOT);

            List<Double> values = new ArrayList<>();
            while (scanner.hasNextDouble()) {
                values.add(scanner.nextDouble());
            }

            return List.copyOf(values);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public double generateValue() {
        int nextIndex = indexPointer.getAndUpdate(i -> {
            int next = i + 1;
            return (next >= mockData.size()) ? 0 : next;
        });

        return mockData.get(nextIndex);
    }
}
