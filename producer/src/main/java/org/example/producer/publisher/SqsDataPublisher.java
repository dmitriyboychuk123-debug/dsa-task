package org.example.producer.publisher;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.example.producer.config.SqsProperties;
import org.springframework.stereotype.Component;

@Component
public class SqsDataPublisher implements DataPublisher {
    private static final String REMOTE_TELEMETRY = "remote-telemetry";
    private final SqsProperties sqsProperties;
    private final SqsTemplate sqsTemplate;

    public SqsDataPublisher(SqsProperties sqsProperties, SqsTemplate sqsTemplate) {
        this.sqsProperties = sqsProperties;
        this.sqsTemplate = sqsTemplate;
    }


    @Override
    public void publish(String data) {
        sqsTemplate.send(to -> to.queue(sqsProperties.remoteTelemetry().name())
                .messageGroupId(REMOTE_TELEMETRY)
                .payload(data));
    }
}
