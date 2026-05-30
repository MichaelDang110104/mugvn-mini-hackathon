package com.hackathon.backend.jobs;

import com.hackathon.backend.models.RecommendationProfile;
import com.hackathon.backend.repositories.RecommendationProfileRepository;
import com.hackathon.backend.repositories.UserOnboardingAnswersRepository;
import com.hackathon.backend.services.OnboardingProfileService;
import com.hackathon.backend.services.StarterRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class StarterRecommendationRefreshJob {

    private final RecommendationProfileRepository profileRepository;
    private final UserOnboardingAnswersRepository answersRepository;
    private final StarterRecommendationService starterRecommendationService;

    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void refreshEligibleProfiles() {
        for (RecommendationProfile profile : profileRepository.findAll()) {
            if (!shouldRefresh(profile)) {
                continue;
            }
            String freeText = answersRepository.findTopBySessionIdOrderByCompletedAtDesc(profile.getSessionId())
                    .map(answer -> answer.getFreeTextTasteSummary())
                    .orElse(null);
            starterRecommendationService.refreshStarterCandidates(profile, freeText);
            profileRepository.save(OnboardingProfileService.markProfileRefreshed(profile));
        }
    }

    boolean shouldRefresh(RecommendationProfile profile) {
        if (profile.getPendingStrongPositiveEvents() != null && profile.getPendingStrongPositiveEvents() >= 3) {
            return true;
        }
        return profile.getLastProfileRefreshAt() == null
                || profile.getLastProfileRefreshAt().isBefore(Instant.now().minusSeconds(86400));
    }
}
