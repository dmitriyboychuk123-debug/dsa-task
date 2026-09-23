package org.example.consumer.listener;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.example.consumer.service.AnomalyDetectionService;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;


@Component
public class RemoteTelemetryQueueListener {
    private final AnomalyDetectionService anomalyDetectionService;

    public RemoteTelemetryQueueListener(AnomalyDetectionService anomalyDetectionService) {
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @SqsListener("${sqs.queues.remote-telemetry.name}")
    public void remoteTelemetry(Message<Double> message) {
        double value = message.getPayload();
        anomalyDetectionService.registerValue(value);
    }
}
