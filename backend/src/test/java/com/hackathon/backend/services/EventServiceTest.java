package com.hackathon.backend.services;

import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.dto.EventResponse;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.UserEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private UserEventRepository userEventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void processEventAcceptsRateEventsAndPersistsEnumValue() {
        EventRequest request = EventRequest.builder()
                .sessionId("session-1")
                .eventId("evt-1")
                .eventType("rate")
                .movieId("movie-1")
                .eventValue(5)
                .timestamp("2026-05-17T11:00:00Z")
                .metadata(Map.of("source", "movie_detail"))
                .build();

        when(userEventRepository.findByEventId("evt-1")).thenReturn(Optional.empty());
        when(userEventRepository.save(any(UserEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.processEvent(request);

        assertTrue(response.isAccepted());
        assertTrue(response.isProfileUpdated());
        assertTrue(response.isRerankedUsingRecentEvents());

        ArgumentCaptor<UserEvent> captor = ArgumentCaptor.forClass(UserEvent.class);
        verify(userEventRepository).save(captor.capture());
        assertEquals(EventType.RATING, captor.getValue().getEventType());
        assertEquals(Instant.parse("2026-05-17T11:00:00Z"), captor.getValue().getTimestamp());
    }

    @Test
    void processEventRejectsInvalidEventType() {
        EventRequest request = EventRequest.builder()
                .sessionId("session-1")
                .eventId("evt-2")
                .eventType("bogus")
                .build();

        EventService.EventValidationException error = assertThrows(
                EventService.EventValidationException.class,
                () -> eventService.processEvent(request));

        assertEquals("Invalid eventType: bogus", error.getMessage());
        verify(userEventRepository, never()).save(any(UserEvent.class));
    }

    @Test
    void processEventReturnsIdempotentSuccessForMatchingDuplicate() {
        EventRequest request = EventRequest.builder()
                .sessionId("session-1")
                .eventId("evt-3")
                .eventType("search")
                .queryText("space opera")
                .build();

        UserEvent existing = UserEvent.builder()
                .eventId("evt-3")
                .sessionId("session-1")
                .eventType(EventType.SEARCH)
                .queryText("space opera")
                .build();

        when(userEventRepository.findByEventId("evt-3")).thenReturn(Optional.of(existing));

        EventResponse response = eventService.processEvent(request);

        assertTrue(response.isAccepted());
        assertFalse(response.isProfileUpdated());
        assertFalse(response.isRerankedUsingRecentEvents());
        verify(userEventRepository, never()).save(any(UserEvent.class));
    }
}
