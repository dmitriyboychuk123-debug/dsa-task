package org.example.producer.schedulingtasks;

import org.example.producer.generator.DataGenerator;
import org.example.producer.publisher.DataPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DataPublishingTask {
    private final DataGenerator dataGenerator;
    private final DataPublisher dataPublisher;

    public DataPublishingTask(DataGenerator dataGenerator, DataPublisher dataPublisher) {
        this.dataGenerator = dataGenerator;
        this.dataPublisher = dataPublisher;
    }

    @Scheduled(fixedRate = 50, timeUnit = TimeUnit.MILLISECONDS)
    public void publishData() {
        double data = dataGenerator.generateValue();
         dataPublisher.publish(String.valueOf(data));
    }
}
