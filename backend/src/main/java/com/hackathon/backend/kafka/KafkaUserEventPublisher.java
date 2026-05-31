package com.hackathon.backend.kafka;

import com.hackathon.backend.dto.EventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaUserEventPublisher implements UserEventPublisher {

    private static final int EVENT_VERSION = 1;

    private final KafkaTemplate<String, UserEventMessageV1> kafkaTemplate;
    private final KafkaTopicsProperties kafkaTopicsProperties;

    @Override
    public void publish(EventRequest request) {
        UserEventMessageV1 message = UserEventMessageV1.builder()
                .eventVersion(EVENT_VERSION)
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .eventId(request.getEventId())
                .eventType(request.getEventType())
                .movieId(request.getMovieId())
                .queryText(request.getQueryText())
                .eventValue(request.getEventValue())
                .metadata(request.getMetadata())
                .timestamp(request.getTimestamp())
                .build();

        kafkaTemplate.send(kafkaTopicsProperties.getUserEventsTopic(), messageKey(message), message).join();
    }

    private String messageKey(UserEventMessageV1 message) {
        if (message.getUserId() != null && !message.getUserId().isBlank()) {
            return message.getUserId();
        }
        return message.getSessionId();
    }
}
