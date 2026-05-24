package com.hackathon.backend.services;

import com.hackathon.backend.dto.OnboardingRequest;
import com.hackathon.backend.models.RecommendationProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingProfileServiceTest {

    @Test
    void buildProfile_createsOnboardingLedProfileWithWeightsAndPreferences() {
        OnboardingRequest request = OnboardingRequest.builder()
                .sessionId("session-1")
                .selectedGenres(List.of("Sci-Fi", "Drama", "Thriller"))
                .selectedThemes(List.of("emotional", "mind-bending", "space"))
                .favoriteTitles(List.of("Interstellar", "Arrival"))
                .avoidedThemes(List.of("gore"))
                .preferredLanguages(List.of("English"))
                .preferredEra("modern")
                .preferredPace("slow")
                .freeTextTasteSummary("Thoughtful emotional sci-fi with strong character relationships")
                .build();

        RecommendationProfile profile = OnboardingProfileService.buildInitialProfile("user-1", request);

        assertThat(profile.getUserId()).isEqualTo("user-1");
        assertThat(profile.getProfileSource()).isEqualTo("onboarding");
        assertThat(profile.getOnboardingWeight()).isEqualTo(1.0);
        assertThat(profile.getBehaviorWeight()).isEqualTo(0.0);
        assertThat(profile.getTopGenres()).containsExactly("Sci-Fi", "Drama", "Thriller");
        assertThat(profile.getTopThemes()).contains("emotional", "mind-bending", "space");
        assertThat(profile.getAvoidedThemes()).containsExactly("gore");
        assertThat(profile.getFavoriteTitles()).containsExactly("Interstellar", "Arrival");
        assertThat(profile.getPreferredEra()).isEqualTo("modern");
        assertThat(profile.getPreferredPace()).isEqualTo("slow");
    }
}
