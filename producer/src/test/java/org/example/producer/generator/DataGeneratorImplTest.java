package org.example.producer.generator;

import org.example.producer.config.GeneratorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataGeneratorImplTest {

    private static final GeneratorProperties PROPERTIES = new GeneratorProperties(50.0, 5.0, 0.01, 6.0, 10.0);

    private RandomGenerator randomGenerator;
    private DataGeneratorImpl generator;

    @BeforeEach
    void setUp() {
        randomGenerator = mock(RandomGenerator.class);
        generator = new DataGeneratorImpl(randomGenerator, PROPERTIES);
    }

    @Test
    void generateValue_returnsGaussianWhenAboveAnomalyProbability() {
        when(randomGenerator.nextDouble()).thenReturn(0.999);
        when(randomGenerator.nextGaussian(50.0, 5.0)).thenReturn(52.3);

        assertEquals(52.3, generator.generateValue());
        verify(randomGenerator, never()).nextDouble(anyDouble(), anyDouble());
    }

    @Test
    void generateValue_returnsPositiveAnomalyWhenBelowAnomalyProbability() {
        when(randomGenerator.nextDouble()).thenReturn(0.0);
        when(randomGenerator.nextDouble(6.0, 10.0)).thenReturn(8.0);
        when(randomGenerator.nextBoolean()).thenReturn(true);

        assertEquals(90.0, generator.generateValue()); // 50 + 8 * 5
        verify(randomGenerator, never()).nextGaussian(anyDouble(), anyDouble());
    }

    @Test
    void generateValue_returnsNegativeAnomalyWhenBelowAnomalyProbability() {
        when(randomGenerator.nextDouble()).thenReturn(0.0);
        when(randomGenerator.nextDouble(6.0, 10.0)).thenReturn(8.0);
        when(randomGenerator.nextBoolean()).thenReturn(false);

        assertEquals(10.0, generator.generateValue()); // 50 - 8 * 5
    }

    @Test
    void generateValue_usesFixedSigmaWhenMinEqualsMax() {
        generator = new DataGeneratorImpl(randomGenerator, new GeneratorProperties(50.0, 5.0, 0.01, 7.0, 7.0));
        when(randomGenerator.nextDouble()).thenReturn(0.0);
        when(randomGenerator.nextBoolean()).thenReturn(true);

        assertEquals(85.0, generator.generateValue()); // 50 + 7 * 5
        verify(randomGenerator, never()).nextDouble(anyDouble(), anyDouble());
    }

    @Test
    void properties_rejectMinSigmaAboveMaxSigma() {
        assertThrows(IllegalArgumentException.class, () -> new GeneratorProperties(50.0, 5.0, 0.01, 10.0, 6.0));
    }
}
