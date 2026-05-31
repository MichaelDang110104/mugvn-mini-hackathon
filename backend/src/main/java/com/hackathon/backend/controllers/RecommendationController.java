package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.ErrorResponse;
import com.hackathon.backend.dto.RecommendationResponse;
import com.hackathon.backend.services.RecommendationService;
import com.hackathon.backend.services.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contract-aligned recommendation endpoint:
 * - GET /api/recommendations
 *
 * Per external-api-contracts.md
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final SessionService sessionService;

    /**
     * GET /api/recommendations
     * Return homepage or context-aware recommendations for a session.
     */
    @GetMapping
    public ResponseEntity<?> getRecommendations(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String context,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String region,
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId) {

        try {
            String resolvedSessionId = sessionService.resolveSessionId(headerSessionId, sessionId);

            RecommendationResponse response = recommendationService.getRecommendations(
                    resolvedSessionId, context, limit, region);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Session-Id", resolvedSessionId);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(response);

        } catch (SessionService.SessionConflictException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.validationError(e.getMessage(),
                            List.of(new ErrorResponse.FieldError("sessionId", "conflict"))));
        } catch (Exception e) {
            log.error("Recommendations failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.internalError("unable to generate recommendations"));
        }
    }
}
