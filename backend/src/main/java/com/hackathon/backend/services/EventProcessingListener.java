package com.hackathon.backend.services;

import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.events.UserEventReceivedEvent;
import com.hackathon.backend.models.AppUser;
import com.hackathon.backend.models.MflixUser;
import com.hackathon.backend.models.RecommendationProfile;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.AppUserRepository;
import com.hackathon.backend.repositories.MflixUserRepository;
import com.hackathon.backend.repositories.RecommendationProfileRepository;
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
    private final RecommendationProfileRepository recommendationProfileRepository;
    private final UserEmbeddingService userEmbeddingService;
    private final ProfileUpdatePolicy profileUpdatePolicy;

    @Async("eventExecutor")
    @EventListener
    public void onEventReceived(UserEventReceivedEvent event) {
        EventRequest request = event.getRequest();
        EventType type = EventType.fromValue(request.getEventType());

        Instant timestamp = request.getTimestamp() != null && !request.getTimestamp().isBlank()
                ? Instant.parse(request.getTimestamp())
                : Instant.now();

        String userId = resolveUserId(request);

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
            return;
        }

        updateProfileCounters(userId, type, request.getEventValue());

        try {
            if (userId != null && profileUpdatePolicy.shouldRecompute(type, request.getEventValue())) {
                userEmbeddingService.computeUserEmbedding(userId);
            }
        } catch (Exception e) {
            log.warn("User embedding computation failed for user [{}]: {}", userId, e.getMessage());
        }
    }

    private void updateProfileCounters(String userId, EventType type, Integer eventValue) {
        if (userId == null || !isStrongPositive(type, eventValue)) {
            return;
        }

        recommendationProfileRepository.findByUserId(userId).ifPresent(profile -> {
            int pending = profile.getPendingStrongPositiveEvents() == null ? 0 : profile.getPendingStrongPositiveEvents();
            int total = profile.getStrongPositiveEventCount() == null ? 0 : profile.getStrongPositiveEventCount();
            profile.setPendingStrongPositiveEvents(pending + 1);
            profile.setStrongPositiveEventCount(total + 1);
            recommendationProfileRepository.save(profile);
        });
    }

    private boolean isStrongPositive(EventType type, Integer eventValue) {
        if (type == EventType.LIKE || type == EventType.SAVE) {
            return true;
        }
        return type == EventType.RATING && eventValue != null && eventValue >= 4;
    }

    private String resolveUserId(EventRequest request) {
        if (request.getUserId() != null) {
            Optional<MflixUser> mflixUser = mflixUserRepository.findByEmail(request.getUserId());
            if (mflixUser.isPresent()) {
                String userId = mflixUser.get().getId().toHexString();
                request.setUserId(userId);
                return userId;
            }
        }

        try {
            Optional<AppUser> appUser = appUserRepository.findBySessionId(request.getSessionId());
            return appUser.map(AppUser::getId).orElse(null);
        } catch (Exception e) {
            log.warn("Could not resolve userId for session [{}]: {}", request.getSessionId(), e.getMessage());
            return null;
        }
    }
}
