package com.hackathon.backend.services;

import com.hackathon.backend.dto.StarterQueryPackage;
import com.hackathon.backend.models.RecommendationProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class StarterQueryService {

    private final EmbeddingService embeddingService;

    public StarterQueryPackage buildForProfile(RecommendationProfile profile, String freeText) {
        return buildPackage(profile, freeText, embeddingService);
    }

    public static StarterQueryPackage buildDeterministicQuery(RecommendationProfile profile, String freeText) {
        return buildPackage(profile, freeText, null);
    }

    private static StarterQueryPackage buildPackage(RecommendationProfile profile, String freeText,
            EmbeddingService embeddingService) {
        Set<String> keywords = new LinkedHashSet<>();
        if (profile.getTopGenres() != null) {
            keywords.addAll(profile.getTopGenres());
        }
        if (profile.getTopThemes() != null) {
            keywords.addAll(profile.getTopThemes());
        }
        if (profile.getFavoriteTitles() != null) {
            keywords.addAll(profile.getFavoriteTitles());
        }
        if (profile.getPreferredLanguages() != null) {
            keywords.addAll(profile.getPreferredLanguages());
        }
        if (profile.getPreferredEra() != null && !profile.getPreferredEra().isBlank()) {
            keywords.add(profile.getPreferredEra());
        }
        if (profile.getPreferredPace() != null && !profile.getPreferredPace().isBlank()) {
            keywords.add(profile.getPreferredPace());
        }

        String profileText = embeddingService != null
                ? embeddingService.buildStarterProfileText(
                        profile.getTopGenres(),
                        profile.getTopThemes(),
                        profile.getFavoriteTitles(),
                        profile.getPreferredLanguages(),
                        profile.getPreferredEra(),
                        profile.getPreferredPace(),
                        freeText)
                : buildProfileTextFallback(profile, freeText);

        return StarterQueryPackage.builder()
                .starterQueryText(profileText)
                .queryKeywords(new ArrayList<>(keywords))
                .querySummary("Starter taste profile: " + profileText)
                .llmModel("deterministic-fallback")
                .queryVersion(1L)
                .build();
    }

    private static String buildProfileTextFallback(RecommendationProfile profile, String freeText) {
        StringBuilder sb = new StringBuilder();
        appendSegment(sb, "Preferred genres", profile.getTopGenres());
        appendSegment(sb, "Themes", profile.getTopThemes());
        appendSegment(sb, "Favorite movies", profile.getFavoriteTitles());
        appendSegment(sb, "Languages", profile.getPreferredLanguages());
        appendValue(sb, "Era", profile.getPreferredEra());
        appendValue(sb, "Pace", profile.getPreferredPace());
        if (freeText != null && !freeText.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(". ");
            }
            sb.append(freeText);
        }
        return sb.toString();
    }

    private static void appendSegment(StringBuilder sb, String label, java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(". ");
        }
        sb.append(label).append(": ").append(String.join(", ", values));
    }

    private static void appendValue(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(". ");
        }
        sb.append(label).append(": ").append(value);
    }
}
