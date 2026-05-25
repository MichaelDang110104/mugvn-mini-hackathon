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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        if (event == null || event.getRequests() == null || event.getRequests().isEmpty()) {
            return;
        }

        Map<String, String> sessionToUserId = new HashMap<>();
        Map<String, Integer> strongPositivesPerUser = new HashMap<>();
        Set<String> usersToRecompute = new HashSet<>();

        for (EventRequest request : event.getRequests()) {
            if (request == null) continue;

            EventType type;
            try {
                type = EventType.fromValue(request.getEventType());
            } catch (IllegalArgumentException e) {
                continue;
            }

            Instant timestamp = request.getTimestamp() != null && !request.getTimestamp().isBlank()
                    ? Instant.parse(request.getTimestamp())
                    : Instant.now();

            String userId = resolveUserIdCached(request, sessionToUserId);

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
                continue;
            }

            if (userId != null && isStrongPositive(type, request.getEventValue())) {
                strongPositivesPerUser.merge(userId, 1, Integer::sum);
            }

            if (userId != null && profileUpdatePolicy.shouldRecompute(type, request.getEventValue())) {
                usersToRecompute.add(userId);
            }
        }

        updateProfileCountersBatch(strongPositivesPerUser);

        for (String userId : usersToRecompute) {
            try {
                userEmbeddingService.computeUserEmbedding(userId);
            } catch (Exception e) {
                log.warn("User embedding computation failed for user [{}]: {}", userId, e.getMessage());
            }
        }
    }

    private void updateProfileCountersBatch(Map<String, Integer> strongPositivesPerUser) {
        if (strongPositivesPerUser == null || strongPositivesPerUser.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Integer> entry : strongPositivesPerUser.entrySet()) {
            String userId = entry.getKey();
            Integer inc = entry.getValue();
            if (userId == null || inc == null || inc <= 0) continue;

            recommendationProfileRepository.findByUserId(userId).ifPresent(profile -> {
                int pending = profile.getPendingStrongPositiveEvents() == null ? 0 : profile.getPendingStrongPositiveEvents();
                int total = profile.getStrongPositiveEventCount() == null ? 0 : profile.getStrongPositiveEventCount();
                profile.setPendingStrongPositiveEvents(pending + inc);
                profile.setStrongPositiveEventCount(total + inc);
                recommendationProfileRepository.save(profile);
            });
        }
    }

    private boolean isStrongPositive(EventType type, Integer eventValue) {
        if (type == EventType.LIKE || type == EventType.SAVE) {
            return true;
        }
        return type == EventType.RATING && eventValue != null && eventValue >= 4;
    }

    private String resolveUserIdCached(EventRequest request, Map<String, String> sessionToUserId) {
        if (request == null) return null;

        if (request.getUserId() != null) {
            Optional<MflixUser> mflixUser = mflixUserRepository.findByEmail(request.getUserId());
            if (mflixUser.isPresent()) {
                String userId = mflixUser.get().getId().toHexString();
                request.setUserId(userId);
                return userId;
            }
        }

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) return null;

        if (sessionToUserId.containsKey(sessionId)) {
            return sessionToUserId.get(sessionId);
        }

        try {
            Optional<AppUser> appUser = appUserRepository.findBySessionId(sessionId);
            String userId = appUser.map(AppUser::getId).orElse(null);
            sessionToUserId.put(sessionId, userId);
            return userId;
        } catch (Exception e) {
            log.warn("Could not resolve userId for session [{}]: {}", sessionId, e.getMessage());
            sessionToUserId.put(sessionId, null);
            return null;
        }
    }
}
