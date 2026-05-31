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
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserEventProcessorTest {

    @Mock
    private UserEventRepository userEventRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private MflixUserRepository mflixUserRepository;

    @Mock
    private RecommendationProfileRepository recommendationProfileRepository;

    @Mock
    private UserEmbeddingService userEmbeddingService;

    @Mock
    private ProfileUpdatePolicy profileUpdatePolicy;

    @InjectMocks
    private UserEventProcessor processor;

    @Test
    void process_persistsEventAndUpdatesProfileForStrongPositive() {
        when(mflixUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(MflixUser.builder()
                .id(new ObjectId("507f1f77bcf86cd799439011"))
                .email("user@example.com")
                .build()));
        RecommendationProfile profile = RecommendationProfile.builder()
                .userId("507f1f77bcf86cd799439011")
                .strongPositiveEventCount(2)
                .pendingStrongPositiveEvents(1)
                .build();
        when(recommendationProfileRepository.findByUserId("507f1f77bcf86cd799439011")).thenReturn(Optional.of(profile));
        when(profileUpdatePolicy.shouldRecompute(EventType.LIKE, null)).thenReturn(true);

        UserEventMessageV1 message = UserEventMessageV1.builder()
                .eventVersion(1)
                .eventId("evt-1")
                .sessionId("session-1")
                .userId("user@example.com")
                .eventType("like")
                .movieId("movie-1")
                .timestamp("2026-05-26T10:15:30Z")
                .metadata(Map.of("source", "detail"))
                .build();

        processor.process(message);

        ArgumentCaptor<UserEvent> eventCaptor = ArgumentCaptor.forClass(UserEvent.class);
        verify(userEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo("evt-1");
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo("507f1f77bcf86cd799439011");
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(EventType.LIKE);
        assertThat(eventCaptor.getValue().getTimestamp()).isEqualTo(Instant.parse("2026-05-26T10:15:30Z"));

        ArgumentCaptor<RecommendationProfile> profileCaptor = ArgumentCaptor.forClass(RecommendationProfile.class);
        verify(recommendationProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getStrongPositiveEventCount()).isEqualTo(3);
        assertThat(profileCaptor.getValue().getPendingStrongPositiveEvents()).isEqualTo(2);

        verify(userEmbeddingService).computeUserEmbedding("507f1f77bcf86cd799439011");
    }

    @Test
    void process_skipsSideEffectsForDuplicateEvent() {
        when(userEventRepository.save(any(UserEvent.class))).thenThrow(new DuplicateKeyException("dup"));

        UserEventMessageV1 message = UserEventMessageV1.builder()
                .eventVersion(1)
                .eventId("evt-1")
                .sessionId("session-1")
                .eventType("like")
                .build();

        processor.process(message);

        verify(recommendationProfileRepository, never()).save(any());
        verify(userEmbeddingService, never()).computeUserEmbedding(any());
    }

    @Test
    void process_resolvesUserIdFromSessionWhenMessageDoesNotIncludeUser() {
        when(appUserRepository.findBySessionId("session-1")).thenReturn(Optional.of(AppUser.builder()
                .id("user-123")
                .sessionId("session-1")
                .build()));
        when(profileUpdatePolicy.shouldRecompute(EventType.VIEW, null)).thenReturn(false);

        UserEventMessageV1 message = UserEventMessageV1.builder()
                .eventVersion(1)
                .eventId("evt-2")
                .sessionId("session-1")
                .eventType("view")
                .build();

        processor.process(message);

        ArgumentCaptor<UserEvent> eventCaptor = ArgumentCaptor.forClass(UserEvent.class);
        verify(userEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo("user-123");
        verify(userEmbeddingService, never()).computeUserEmbedding(any());
    }
}
