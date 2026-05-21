package com.hackathon.backend.services;

import com.hackathon.backend.dto.EventRequest;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.events.UserEventReceivedEvent;
import com.hackathon.backend.models.AppUser;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.AppUserRepository;
import com.hackathon.backend.repositories.MflixUserRepository;
import com.hackathon.backend.repositories.UserEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventProcessingListenerTest {

    @Mock
    private UserEventRepository userEventRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private MflixUserRepository mflixUserRepository;

    @Mock
    private UserEmbeddingService userEmbeddingService;

    @Mock
    private ProfileUpdatePolicy profileUpdatePolicy;

    @InjectMocks
    private EventProcessingListener listener;

    @Test
    void onEventReceived_savesWeakSignalsWithoutRecomputingProfile() {
        EventRequest request = EventRequest.builder()
                .eventId("evt-1")
                .sessionId("session-1")
                .eventType("view")
                .movieId("507f1f77bcf86cd799439011")
                .timestamp(Instant.now().toString())
                .build();

        when(appUserRepository.findBySessionId("session-1"))
                .thenReturn(Optional.of(AppUser.builder().id("user-1").sessionId("session-1").build()));
        when(profileUpdatePolicy.shouldRecompute(EventType.VIEW, null)).thenReturn(false);

        listener.onEventReceived(new UserEventReceivedEvent(this, request));

        ArgumentCaptor<UserEvent> eventCaptor = ArgumentCaptor.forClass(UserEvent.class);
        verify(userEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(EventType.VIEW);
        verify(userEmbeddingService, never()).computeUserEmbedding("user-1");
    }

    @Test
    void onEventReceived_recomputesProfileUsingResolvedUserIdForStrongSignals() {
        EventRequest request = EventRequest.builder()
                .eventId("evt-2")
                .sessionId("session-2")
                .eventType("like")
                .movieId("507f1f77bcf86cd799439012")
                .timestamp(Instant.now().toString())
                .build();

        when(appUserRepository.findBySessionId("session-2"))
                .thenReturn(Optional.of(AppUser.builder().id("user-2").sessionId("session-2").build()));
        when(profileUpdatePolicy.shouldRecompute(EventType.LIKE, null)).thenReturn(true);

        listener.onEventReceived(new UserEventReceivedEvent(this, request));

        ArgumentCaptor<UserEvent> eventCaptor = ArgumentCaptor.forClass(UserEvent.class);
        verify(userEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo("user-2");
        verify(userEmbeddingService).computeUserEmbedding("user-2");
    }
}
