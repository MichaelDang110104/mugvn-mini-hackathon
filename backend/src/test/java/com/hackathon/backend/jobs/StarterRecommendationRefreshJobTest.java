package com.hackathon.backend.jobs;

import com.hackathon.backend.models.RecommendationProfile;
import com.hackathon.backend.models.UserOnboardingAnswers;
import com.hackathon.backend.repositories.RecommendationProfileRepository;
import com.hackathon.backend.repositories.UserOnboardingAnswersRepository;
import com.hackathon.backend.services.StarterRecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StarterRecommendationRefreshJobTest {

    @Mock
    private RecommendationProfileRepository profileRepository;

    @Mock
    private UserOnboardingAnswersRepository answersRepository;

    @Mock
    private StarterRecommendationService starterRecommendationService;

    @InjectMocks
    private StarterRecommendationRefreshJob job;

    @Test
    void refreshEligibleProfiles_recomputesWhenPendingEventsReachThreshold() {
        RecommendationProfile profile = RecommendationProfile.builder()
                .userId("user-1")
                .sessionId("session-1")
                .pendingStrongPositiveEvents(3)
                .build();
        when(profileRepository.findAll()).thenReturn(List.of(profile));
        when(answersRepository.findTopBySessionIdOrderByCompletedAtDesc("session-1"))
                .thenReturn(Optional.of(UserOnboardingAnswers.builder().freeTextTasteSummary("sci-fi").build()));

        job.refreshEligibleProfiles();

        verify(starterRecommendationService).refreshStarterCandidates(profile, "sci-fi");
    }
}
