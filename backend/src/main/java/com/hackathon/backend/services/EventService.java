package com.hackathon.backend.services;

import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.dto.EventResponse;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.UserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Handles event ingestion, validation, idempotency checks, and persistence.
 * Implements all rules from the event-and-error-contracts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final UserEventRepository userEventRepository;

    private static final Set<String> VALID_EVENT_TYPES = Set.of(
            "search", "view", "click", "like", "save", "rate"
    );

    private static final Set<String> REQUIRES_MOVIE_ID = Set.of(
            "like", "save", "view", "click", "rate"
    );

    /**
     * Process a single event request.
     * Validates, checks idempotency, persists, and returns response.
     */
    public EventResponse processEvent(EventRequest request) {
        // Validate event type
        if (!VALID_EVENT_TYPES.contains(request.getEventType())) {
            throw new EventValidationException("Invalid eventType: " + request.getEventType(),
                    List.of(new FieldError("eventType", "invalid")));
        }

        // Validate search requires queryText
        if ("search".equals(request.getEventType())
                && (request.getQueryText() == null || request.getQueryText().isBlank())) {
            throw new EventValidationException("queryText is required for search events",
                    List.of(new FieldError("queryText", "required")));
        }

        // Validate movieId-required event types
        if (REQUIRES_MOVIE_ID.contains(request.getEventType())
                && (request.getMovieId() == null || request.getMovieId().isBlank())) {
            throw new EventValidationException(
                    "movieId is required for " + request.getEventType() + " events",
                    List.of(new FieldError("movieId", "required")));
        }

        // Validate rate scale
        if ("rate".equals(request.getEventType())) {
            if (request.getEventValue() == null) {
                throw new EventValidationException("eventValue is required for rate events",
                        List.of(new FieldError("eventValue", "required")));
            }
            if (request.getEventValue() < 1 || request.getEventValue() > 5) {
                throw new EventValidationException(
                        "eventValue must be between 1 and 5 for rate events",
                        List.of(new FieldError("eventValue", "out_of_range")));
            }
        }

        // Validate timestamp format if present
        Instant eventTimestamp;
        if (request.getTimestamp() != null && !request.getTimestamp().isBlank()) {
            try {
                eventTimestamp = Instant.parse(request.getTimestamp());
            } catch (Exception e) {
                throw new EventValidationException("timestamp must be a valid ISO-8601 timestamp",
                        List.of(new FieldError("timestamp", "invalid_format")));
            }
        } else {
            // Backend assigns timestamp
            eventTimestamp = Instant.now();
        }

        // Idempotency check
        Optional<UserEvent> existing = userEventRepository.findByEventId(request.getEventId());
        if (existing.isPresent()) {
            UserEvent existingEvent = existing.get();
            // Check for conflicting payload
            if (isConflictingDuplicate(existingEvent, request)) {
                throw new EventConflictException(
                        "Duplicate eventId with conflicting payload: " + request.getEventId());
            }
            // Same payload — idempotent success
            log.info("Idempotent duplicate event [{}], returning success", request.getEventId());
            return EventResponse.builder()
                    .accepted(true)
                    .profileUpdated(false)
                    .rerankedUsingRecentEvents(false)
                    .build();
        }

        // Persist the event
        UserEvent event = UserEvent.builder()
                .eventId(request.getEventId())
                .sessionId(request.getSessionId())
                .eventType(request.getEventType())
                .movieId(request.getMovieId())
                .queryText(request.getQueryText())
                .eventValue(request.getEventValue())
                .metadata(request.getMetadata())
                .timestamp(eventTimestamp)
                .build();

        userEventRepository.save(event);
        log.info("Persisted event [{}] type=[{}] session=[{}]",
                event.getEventId(), event.getEventType(), event.getSessionId());

        // Determine if this is a profile-influencing event
        boolean isPositiveSignal = isPositiveSignal(request.getEventType(), request.getEventValue());

        return EventResponse.builder()
                .accepted(true)
                .profileUpdated(isPositiveSignal)
                .rerankedUsingRecentEvents(isPositiveSignal)
                .build();
    }

    /**
     * Per contract: 4 and 5 are positive, 3 is neutral, 1 and 2 are low/negative.
     * like and save are always positive signals.
     */
    private boolean isPositiveSignal(String eventType, Integer eventValue) {
        if ("like".equals(eventType) || "save".equals(eventType)) {
            return true;
        }
        if ("rate".equals(eventType) && eventValue != null) {
            return eventValue >= 4;
        }
        return false;
    }

    /**
     * Compare existing event with incoming request to detect conflicting duplicate.
     * Ignores server-assigned timestamp per contract.
     */
    private boolean isConflictingDuplicate(UserEvent existing, EventRequest request) {
        return !Objects.equals(existing.getSessionId(), request.getSessionId())
                || !Objects.equals(existing.getEventType(), request.getEventType())
                || !Objects.equals(existing.getMovieId(), request.getMovieId())
                || !Objects.equals(existing.getQueryText(), request.getQueryText())
                || !Objects.equals(existing.getEventValue(), request.getEventValue());
    }

    // --- Custom exceptions ---

    public static class EventValidationException extends RuntimeException {
        private final List<FieldError> details;

        public EventValidationException(String message, List<FieldError> details) {
            super(message);
            this.details = details;
        }

        public List<FieldError> getDetails() {
            return details;
        }
    }

    public static class EventConflictException extends RuntimeException {
        public EventConflictException(String message) {
            super(message);
        }
    }

    public record FieldError(String field, String reason) {}
}
