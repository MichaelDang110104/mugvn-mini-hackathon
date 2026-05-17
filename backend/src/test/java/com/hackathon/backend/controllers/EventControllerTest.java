package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.dto.EventResponse;
import com.hackathon.backend.dto.ErrorResponse;
import com.hackathon.backend.services.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @Test
    void postEventReturnsAcceptedBodyUsedByFrontend() {
        EventRequest request = EventRequest.builder()
                .sessionId("session-1")
                .eventId("evt-1")
                .eventType("rate")
                .movieId("movie-1")
                .eventValue(5)
                .build();

        when(eventService.processEvent(request)).thenReturn(EventResponse.builder()
                .accepted(true)
                .profileUpdated(true)
                .rerankedUsingRecentEvents(true)
                .build());

        ResponseEntity<?> response = eventController.postEvent(request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        EventResponse body = (EventResponse) response.getBody();
        assertTrue(body.isAccepted());
        assertTrue(body.isProfileUpdated());
        assertTrue(body.isRerankedUsingRecentEvents());
    }

    @Test
    void postEventReturnsValidationErrorWhenServiceRejectsRequest() {
        EventRequest request = EventRequest.builder()
                .sessionId("session-1")
                .eventId("evt-2")
                .eventType("bogus")
                .build();

        when(eventService.processEvent(request)).thenThrow(
                new EventService.EventValidationException(
                        "Invalid eventType: bogus",
                        List.of(new EventService.FieldError("eventType", "invalid"))));

        ResponseEntity<?> response = eventController.postEvent(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("VALIDATION_ERROR", body.getError().getCode());
    }

    @Test
    void postEventsBatchCountsAcceptedAndFailedEvents() {
        EventRequest valid = EventRequest.builder()
                .sessionId("session-1")
                .eventId("evt-1")
                .eventType("view")
                .build();
        EventRequest invalid = EventRequest.builder()
                .sessionId("session-1")
                .eventId("evt-2")
                .eventType("bogus")
                .build();

        when(eventService.processEvent(invalid)).thenThrow(
                new EventService.EventValidationException(
                        "Invalid eventType: bogus",
                        List.of(new EventService.FieldError("eventType", "invalid"))));

        ResponseEntity<?> response = eventController.postEventsBatch(List.of(valid, invalid));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Integer> body = (Map<String, Integer>) response.getBody();
        assertEquals(1, body.get("accepted"));
        assertEquals(1, body.get("failed"));
    }
}
