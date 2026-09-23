package org.example.producer.schedulingtasks;

import org.example.producer.generator.DataGenerator;
import org.example.producer.publisher.DataPublisher;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPublishingTaskTest {

    @Test
    void publishData_publishesGeneratedValueAsString() {
        DataGenerator generator = mock(DataGenerator.class);
        DataPublisher publisher = mock(DataPublisher.class);
        when(generator.generateValue()).thenReturn(-1.75);

        new DataPublishingTask(generator, publisher).publishData();

        verify(publisher).publish("-1.75");
    }
}
