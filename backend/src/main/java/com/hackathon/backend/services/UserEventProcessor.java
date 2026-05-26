package com.hackathon.backend.services;

import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.kafka.UserEventMessageV1;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventProcessor {

    private final UserEventRepository userEventRepository;
    private final AppUserRepository appUserRepository;
    private final MflixUserRepository mflixUserRepository;
    private final RecommendationProfileRepository recommendationProfileRepository;
    private final UserEmbeddingService userEmbeddingService;
    private final ProfileUpdatePolicy profileUpdatePolicy;

    public void process(UserEventMessageV1 message) {
        if (message == null) {
            return;
        }

        EventType type = EventType.fromValue(message.getEventType());
        Instant timestamp = parseTimestamp(message.getTimestamp());
        String userId = resolveUserId(message);

        UserEvent userEvent = UserEvent.builder()
                .eventId(message.getEventId())
                .userId(userId)
                .sessionId(message.getSessionId())
                .eventType(type)
                .movieId(message.getMovieId())
                .queryText(message.getQueryText())
                .eventValue(message.getEventValue())
                .metadata(message.getMetadata())
                .timestamp(timestamp)
                .build();

        try {
            userEventRepository.save(userEvent);
            log.debug("Persisted event [{}] type=[{}] weight={} session=[{}]",
                    userEvent.getEventId(), type.getValue(), type.getWeight(), userEvent.getSessionId());
        } catch (DuplicateKeyException e) {
            log.debug("Duplicate event [{}], skipping", message.getEventId());
            return;
        }

        if (userId != null && isStrongPositive(type, message.getEventValue())) {
            recommendationProfileRepository.findByUserId(userId).ifPresent(profile -> {
                int pending = profile.getPendingStrongPositiveEvents() == null ? 0 : profile.getPendingStrongPositiveEvents();
                int total = profile.getStrongPositiveEventCount() == null ? 0 : profile.getStrongPositiveEventCount();
                profile.setPendingStrongPositiveEvents(pending + 1);
                profile.setStrongPositiveEventCount(total + 1);
                recommendationProfileRepository.save(profile);
            });
        }

        if (userId != null && profileUpdatePolicy.shouldRecompute(type, message.getEventValue())) {
            try {
                userEmbeddingService.computeUserEmbedding(userId);
            } catch (Exception e) {
                log.warn("User embedding computation failed for user [{}]: {}", userId, e.getMessage());
            }
        }
    }

    private Instant parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return Instant.now();
        }
        return Instant.parse(timestamp);
    }

    private boolean isStrongPositive(EventType type, Integer eventValue) {
        if (type == EventType.LIKE || type == EventType.SAVE) {
            return true;
        }
        return type == EventType.RATING && eventValue != null && eventValue >= 4;
    }

    private String resolveUserId(UserEventMessageV1 message) {
        if (message.getUserId() != null && !message.getUserId().isBlank()) {
            Optional<MflixUser> mflixUser = mflixUserRepository.findByEmail(message.getUserId());
            if (mflixUser.isPresent()) {
                return mflixUser.get().getId().toHexString();
            }
            return message.getUserId();
        }

        if (message.getSessionId() == null || message.getSessionId().isBlank()) {
            return null;
        }

        try {
            return appUserRepository.findBySessionId(message.getSessionId()).map(AppUser::getId).orElse(null);
        } catch (Exception e) {
            log.warn("Could not resolve userId for session [{}]: {}", message.getSessionId(), e.getMessage());
            return null;
        }
    }
}
