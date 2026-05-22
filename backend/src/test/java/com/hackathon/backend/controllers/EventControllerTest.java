package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.events.UserEventReceivedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EventControllerTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final EventController controller = new EventController(eventPublisher);

    @Test
    void postEventsBatch_acceptsAndPublishesAllValidEvents() {
        ResponseEntity<?> response = controller.postEventsBatch(List.of(
                request("evt-1", "view"),
                request("evt-2", "like")
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("accepted", 2);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("failed", 0);
        verify(eventPublisher, times(2)).publishEvent(any(UserEventReceivedEvent.class));
    }

    @Test
    void postEventsBatch_countsInvalidEventsWithoutBlockingValidOnes() {
        ResponseEntity<?> response = controller.postEventsBatch(List.of(
                request("evt-1", "view"),
                request("evt-2", "unknown")
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("accepted", 1);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("failed", 1);
        verify(eventPublisher, times(1)).publishEvent(any(UserEventReceivedEvent.class));
    }

    private EventRequest request(String eventId, String eventType) {
        return EventRequest.builder()
                .sessionId("session-1")
                .eventId(eventId)
                .eventType(eventType)
                .movieId("movie-1")
                .build();
    }
}
