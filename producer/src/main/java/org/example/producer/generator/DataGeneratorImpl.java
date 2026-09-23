package org.example.producer.generator;

import org.example.producer.config.GeneratorProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.random.RandomGenerator;

@Component
@ConditionalOnProperty(
        name = "app.features.mock-data-generator.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class DataGeneratorImpl implements DataGenerator {
    private final RandomGenerator randomGenerator;
    private final GeneratorProperties properties;

    public DataGeneratorImpl(RandomGenerator randomGenerator, GeneratorProperties properties) {
        this.randomGenerator = randomGenerator;
        this.properties = properties;
    }

    @Override
    public double generateValue() {
        if (randomGenerator.nextDouble() < properties.anomalyProbability()) {
            return generateAnomaly();
        }

        return randomGenerator.nextGaussian(properties.mean(), properties.standardDeviation());
    }

    private double generateAnomaly() {
        double sigmas = properties.anomalyMinSigma() == properties.anomalyMaxSigma()
                ? properties.anomalyMinSigma()
                : randomGenerator.nextDouble(properties.anomalyMinSigma(), properties.anomalyMaxSigma());
        double direction = randomGenerator.nextBoolean() ? 1 : -1;

        return properties.mean() + (direction * sigmas * properties.standardDeviation());
    }
}
