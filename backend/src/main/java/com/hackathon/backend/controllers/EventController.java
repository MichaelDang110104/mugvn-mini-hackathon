package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.ErrorResponse;
import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.dto.EventResponse;
import com.hackathon.backend.services.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<?> postEvent(@RequestBody EventRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.validationError("sessionId is required",
                            List.of(new ErrorResponse.FieldError("sessionId", "required"))));
        }
        if (request.getEventId() == null || request.getEventId().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.validationError("eventId is required",
                            List.of(new ErrorResponse.FieldError("eventId", "required"))));
        }
        if (request.getEventType() == null || request.getEventType().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.validationError("eventType is required",
                            List.of(new ErrorResponse.FieldError("eventType", "required"))));
        }

        try {
            EventResponse response = eventService.processEvent(request);
            log.debug("Accepted event [{}] type=[{}] session=[{}]",
                    request.getEventId(), request.getEventType(), request.getSessionId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (EventService.EventValidationException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.validationError(e.getMessage(), toErrorFields(e.getDetails())));
        } catch (EventService.EventConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.validationError(e.getMessage(),
                            List.of(new ErrorResponse.FieldError("eventId", "conflict"))));
        }
    }

    /**
     * POST /api/events/batch
     * Accept a batch of events. Process each individually.
     * Returns count of accepted and failed events.
     */
    @PostMapping("/batch")
    public ResponseEntity<?> postEventsBatch(@RequestBody List<EventRequest> requests) {
        int accepted = 0;
        int failed = 0;

        for (EventRequest request : requests) {
            try {
                if (request.getSessionId() == null || request.getEventId() == null
                        || request.getEventType() == null) {
                    failed++;
                    continue;
                }
                eventService.processEvent(request);
                accepted++;
            } catch (EventService.EventValidationException | EventService.EventConflictException e) {
                log.warn("Batch event failed [{}]: {}", request.getEventId(), e.getMessage());
                failed++;
            } catch (Exception e) {
                log.warn("Batch event failed [{}]: {}", request.getEventId(), e.getMessage());
                failed++;
            }
        }

        return ResponseEntity.ok(java.util.Map.of(
                "accepted", accepted,
                "failed", failed));
    }

    private List<ErrorResponse.FieldError> toErrorFields(List<EventService.FieldError> details) {
        return details.stream()
                .map(detail -> new ErrorResponse.FieldError(detail.field(), detail.reason()))
                .collect(Collectors.toList());
    }
}
