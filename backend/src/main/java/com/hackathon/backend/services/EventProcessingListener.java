package com.hackathon.backend.services;

import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.events.UserEventReceivedEvent;
import com.hackathon.backend.models.AppUser;
import com.hackathon.backend.models.MflixUser;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.AppUserRepository;
import com.hackathon.backend.repositories.MflixUserRepository;
import com.hackathon.backend.repositories.UserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProcessingListener {

    private final UserEventRepository userEventRepository;
    private final AppUserRepository appUserRepository;
    private final MflixUserRepository mflixUserRepository;
    private final UserEmbeddingService userEmbeddingService;

    @Async("eventExecutor")
    @EventListener
    public void onEventReceived(UserEventReceivedEvent event) {
        EventRequest request = event.getRequest();
        EventType type = EventType.fromValue(request.getEventType());

        Instant timestamp = request.getTimestamp() != null && !request.getTimestamp().isBlank()
                ? Instant.parse(request.getTimestamp())
                : Instant.now();

        String userId = null;
        String username = null;
        if (request.getUserId() != null) {
            // Resolve actual user ID and username from the email passed via Spring Security context
            Optional<MflixUser> mflixUser = mflixUserRepository.findByEmail(request.getUserId());
            if (mflixUser.isPresent()) {
                request.setUserId(mflixUser.get().getId().toHexString());
                userId = mflixUser.get().getId().toHexString();
                username = mflixUser.get().getName();
            }
        } else {
            try {
                Optional<AppUser> appUser = appUserRepository.findBySessionId(request.getSessionId());
                userId = appUser.map(AppUser::getId).orElse(null);
            } catch (Exception e) {
                log.warn("Could not resolve userId for session [{}]: {}", request.getSessionId(), e.getMessage());
            }
        }

        UserEvent userEvent = UserEvent.builder()
                .eventId(request.getEventId())
                .userId(userId)
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

        try {
            if (username != null && isStrongEvent(type, request.getEventValue())) {
                userEmbeddingService.computeUserEmbedding(username);
            }
        } catch (Exception e) {
            log.warn("User embedding computation failed for user [{}]: {}", username, e.getMessage());
        }
    }

    private boolean isStrongEvent(EventType type, Integer eventValue) {
        if (type == EventType.LIKE || type == EventType.SAVE || type == EventType.WATCH_START) {
            return true;
        }
        if (type == EventType.RATING && eventValue != null && eventValue >= 4) {
            return true;
        }
        return false;
    }
}
