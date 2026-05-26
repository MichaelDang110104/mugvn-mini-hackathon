package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.kafka.UserEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private UserEventPublisher userEventPublisher;

    @InjectMocks
    private EventController controller;

    @Test
    void postEvent_returnsAcceptedWhenPublishSucceeds() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                User.withUsername("user@example.com").password("pw").authorities(List.of()).build(),
                null));

        EventRequest request = EventRequest.builder()
                .sessionId("session-1")
                .eventId("evt-1")
                .eventType("like")
                .metadata(Map.of("source", "detail"))
                .build();

        ResponseEntity<?> response = controller.postEvent(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(userEventPublisher).publish(any(EventRequest.class));
        assertThat(request.getUserId()).isEqualTo("user@example.com");
        SecurityContextHolder.clearContext();
    }

    @Test
    void postEvent_returnsServiceUnavailableWhenPublishFails() {
        doThrow(new IllegalStateException("Kafka down")).when(userEventPublisher).publish(any(EventRequest.class));

        EventRequest request = EventRequest.builder()
                .sessionId("session-1")
                .eventId("evt-1")
                .eventType("like")
                .build();

        ResponseEntity<?> response = controller.postEvent(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
