package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.ErrorResponse;
import com.hackathon.backend.dto.BatchEventResponse;
import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.dto.EventResponse;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.kafka.UserEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final UserEventPublisher userEventPublisher;

    @PostMapping
    public ResponseEntity<?> postEvent(@RequestBody EventRequest request) {
        ErrorResponse validationError = validate(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(validationError);
        }

        try {
            publish(List.of(request));
        } catch (RuntimeException e) {
            log.warn("Kafka publish failed for event [{}]: {}", request.getEventId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse.internalError("Event ingestion is temporarily unavailable"));
        }

        return ResponseEntity.accepted().body(new EventResponse("ack"));
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchEventResponse> postEventsBatch(@RequestBody List<EventRequest> requests) {
        int accepted = 0;
        int failed = 0;

        List<EventRequest> valid = new ArrayList<>();
        for (EventRequest request : requests) {
            if (validate(request) != null) {
                failed++;
                continue;
            }
            valid.add(request);
        }

        if (!valid.isEmpty()) {
            accepted = valid.size();
            try {
                publish(valid);
            } catch (RuntimeException e) {
                log.warn("Kafka publish failed for batch event request: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new BatchEventResponse(0, requests.size()));
            }
        }

        return ResponseEntity.accepted().body(new BatchEventResponse(accepted, failed));
    }

    private ErrorResponse validate(EventRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            return ErrorResponse.validationError("sessionId is required",
                    List.of(new ErrorResponse.FieldError("sessionId", "required")));
        }
        if (request.getEventId() == null || request.getEventId().isBlank()) {
            return ErrorResponse.validationError("eventId is required",
                    List.of(new ErrorResponse.FieldError("eventId", "required")));
        }
        if (request.getEventType() == null || request.getEventType().isBlank()) {
            return ErrorResponse.validationError("eventType is required",
                    List.of(new ErrorResponse.FieldError("eventType", "required")));
        }

        try {
            EventType.fromValue(request.getEventType());
            return null;
        } catch (IllegalArgumentException e) {
            return ErrorResponse.validationError("Unknown eventType: " + request.getEventType(),
                    List.of(new ErrorResponse.FieldError("eventType", "invalid")));
        }
    }

    private void publish(List<EventRequest> requests) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            for (EventRequest request : requests) {
                if (request != null) {
                    request.setUserId(userDetails.getUsername());
                }
            }
        }

        if (requests != null) {
            for (EventRequest request : requests) {
                if (request == null) continue;
                log.debug("Received event [{}] type=[{}] session=[{}]",
                        request.getEventId(), request.getEventType(), request.getSessionId());
            }
        }

        for (EventRequest request : requests) {
            userEventPublisher.publish(request);
        }
    }
}
