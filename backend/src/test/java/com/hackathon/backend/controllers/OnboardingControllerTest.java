package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.OnboardingRequest;
import com.hackathon.backend.dto.OnboardingResponse;
import com.hackathon.backend.models.AppUser;
import com.hackathon.backend.models.RecommendationProfile;
import com.hackathon.backend.models.UserOnboardingAnswers;
import com.hackathon.backend.repositories.AppUserRepository;
import com.hackathon.backend.repositories.RecommendationProfileRepository;
import com.hackathon.backend.repositories.UserOnboardingAnswersRepository;
import com.hackathon.backend.services.SessionService;
import com.hackathon.backend.services.StarterRecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserOnboardingAnswersRepository answersRepository;

    @Mock
    private RecommendationProfileRepository profileRepository;

    @Mock
    private StarterRecommendationService starterRecommendationService;

    @InjectMocks
    private OnboardingController controller;

    @Test
    void submitOnboarding_persistsAnswersAndReturnsResolvedSession() {
        when(sessionService.resolveSessionId(null, "session-1")).thenReturn("session-1");
        when(appUserRepository.findBySessionId("session-1")).thenReturn(Optional.of(AppUser.builder()
                .id("user-1")
                .sessionId("session-1")
                .createdAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build()));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileRepository.save(any(RecommendationProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingRequest request = OnboardingRequest.builder()
                .sessionId("session-1")
                .selectedGenres(List.of("Sci-Fi", "Drama", "Thriller"))
                .selectedThemes(List.of("emotional", "space", "mind-bending"))
                .favoriteTitles(List.of("Interstellar", "Arrival"))
                .build();

        OnboardingResponse response = controller.submit(null, request);

        assertThat(response.getSessionId()).isEqualTo("session-1");
        assertThat(response.isCompleted()).isTrue();

        ArgumentCaptor<UserOnboardingAnswers> answersCaptor = ArgumentCaptor.forClass(UserOnboardingAnswers.class);
        verify(answersRepository).save(answersCaptor.capture());
        assertThat(answersCaptor.getValue().getSelectedGenres()).containsExactly("Sci-Fi", "Drama", "Thriller");

        ArgumentCaptor<RecommendationProfile> profileCaptor = ArgumentCaptor.forClass(RecommendationProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getProfileSource()).isEqualTo("onboarding");
        assertThat(profileCaptor.getValue().getUserId()).isEqualTo("user-1");

        verify(starterRecommendationService).refreshStarterCandidates(any(RecommendationProfile.class), any());
    }
}
