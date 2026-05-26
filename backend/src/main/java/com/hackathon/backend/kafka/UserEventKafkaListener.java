package com.hackathon.backend.kafka;

import com.hackathon.backend.services.UserEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventKafkaListener {

    private final UserEventProcessor userEventProcessor;

    @KafkaListener(
            topics = "${app.kafka.user-events-topic}",
            groupId = "${app.kafka.user-event-processor-group}",
            concurrency = "${app.kafka.user-event-consumer-concurrency:1}"
    )
    public void onMessage(UserEventMessageV1 message) {
        userEventProcessor.process(message);
    }
}
