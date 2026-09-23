package org.example.producer.publisher;

import io.awspring.cloud.sqs.operations.SqsSendOptions;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.example.producer.config.SqsProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Consumer;

import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SqsDataPublisherTest {

    @Test
    @SuppressWarnings("unchecked")
    void publish_sendsPayloadToConfiguredQueue() {
        SqsTemplate sqsTemplate = mock(SqsTemplate.class);
        SqsProperties properties = new SqsProperties(new SqsProperties.SqsConfig("remote-telemetry.fifo"));
        SqsDataPublisher publisher = new SqsDataPublisher(properties, sqsTemplate);

        publisher.publish("0.42");

        ArgumentCaptor<Consumer<SqsSendOptions<String>>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(sqsTemplate).send(captor.capture());

        SqsSendOptions<String> options = mock(SqsSendOptions.class, RETURNS_SELF);
        captor.getValue().accept(options);

        verify(options).queue("remote-telemetry.fifo");
        verify(options).messageGroupId("remote-telemetry");
        verify(options).payload("0.42");
    }
}
