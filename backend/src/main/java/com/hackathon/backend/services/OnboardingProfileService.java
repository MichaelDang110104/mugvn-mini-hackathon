package com.hackathon.backend.services;

import com.hackathon.backend.dto.OnboardingRequest;
import com.hackathon.backend.models.RecommendationProfile;

import java.time.Instant;

public final class OnboardingProfileService {

    private OnboardingProfileService() {
    }

    public static RecommendationProfile buildInitialProfile(String userId, OnboardingRequest request) {
        Instant now = Instant.now();

        return RecommendationProfile.builder()
                // Use a stable document identity: one profile doc per user.
                .id(userId)
                .userId(userId)
                .sessionId(request.getSessionId())
                .topGenres(request.getSelectedGenres())
                .topThemes(request.getSelectedThemes())
                .avoidedGenres(request.getAvoidedGenres())
                .avoidedThemes(request.getAvoidedThemes())
                .favoriteTitles(request.getFavoriteTitles())
                .preferredLanguages(request.getPreferredLanguages())
                .preferredEra(request.getPreferredEra())
                .preferredPace(request.getPreferredPace())
                .profileSource("onboarding")
                .onboardingWeight(1.0)
                .behaviorWeight(0.0)
                .strongPositiveEventCount(0)
                .pendingStrongPositiveEvents(0)
                .onboardingCompletedAt(now)
                .lastProfileRefreshAt(now)
                .profileVersion(1L)
                .build();
    }

    public static RecommendationProfile markProfileRefreshed(RecommendationProfile profile) {
        profile.setPendingStrongPositiveEvents(0);
        profile.setLastProfileRefreshAt(Instant.now());
        return profile;
    }
}
