package com.hackathon.backend.services;

import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.events.UserEventReceivedEvent;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.UserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProcessingListener {

    private final UserEventRepository userEventRepository;

    @Async("eventExecutor")
    @EventListener
    public void onEventReceived(UserEventReceivedEvent event) {
        EventRequest request = event.getRequest();
        EventType type = EventType.fromValue(request.getEventType());

        Instant timestamp = request.getTimestamp() != null && !request.getTimestamp().isBlank()
                ? Instant.parse(request.getTimestamp())
                : Instant.now();

        UserEvent userEvent = UserEvent.builder()
                .eventId(request.getEventId())
                .sessionId(request.getSessionId())
                .eventType(type)
                .movieId(request.getMovieId())
                .queryText(request.getQueryText())
                .eventValue(request.getEventValue())
                .metadata(request.getMetadata())
                .timestamp(timestamp)
                .build();

        try {
            userEventRepository.save(userEvent);
            log.debug("Persisted event [{}] type=[{}] weight={} session=[{}]",
                    userEvent.getEventId(), type.getValue(), type.getWeight(), userEvent.getSessionId());
        } catch (DuplicateKeyException e) {
            log.debug("Duplicate event [{}], skipping", request.getEventId());
        }
    }
}
