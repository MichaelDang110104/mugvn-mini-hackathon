package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.ErrorResponse;
import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.dto.EventResponse;
import com.hackathon.backend.services.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contract-aligned event endpoint:
 * - POST /api/events
 *
 * Per external-api-contracts.md and event-and-error-contracts.md
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;

    /**
     * POST /api/events
     * Capture recommendation-relevant user behavior signals.
     */
    @PostMapping
    public ResponseEntity<?> postEvent(@RequestBody EventRequest request) {
        // Basic null checks before service validation
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
            return ResponseEntity.ok(response);

        } catch (EventService.EventValidationException e) {
            List<ErrorResponse.FieldError> details = e.getDetails().stream()
                    .map(fe -> new ErrorResponse.FieldError(fe.field(), fe.reason()))
                    .toList();
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.validationError(e.getMessage(), details));

        } catch (EventService.EventConflictException e) {
            return ResponseEntity.status(409)
                    .body(ErrorResponse.validationError(e.getMessage(),
                            List.of(new ErrorResponse.FieldError("eventId", "conflict"))));

        } catch (Exception e) {
            log.error("Event processing failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.internalError("unable to process event"));
        }
    }
}
