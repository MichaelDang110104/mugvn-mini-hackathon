package com.hackathon.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_profiles")
public class RecommendationProfile {

    @Id
    private String id;

    private String userId;
    private String sessionId;
    private List<Double> profileEmbedding;
    private String lastComputedAt;
    private Integer sourceEventCount;
    private List<String> topGenres;
    private List<String> topThemes;
    private List<String> avoidedGenres;
    private List<String> avoidedThemes;
    private List<String> favoriteTitles;
    private List<String> preferredLanguages;
    private String preferredEra;
    private String preferredPace;
    private String profileSource;
    private Double onboardingWeight;
    private Double behaviorWeight;
    private Integer strongPositiveEventCount;
    private Integer pendingStrongPositiveEvents;
    private Instant onboardingCompletedAt;
    private Instant lastProfileRefreshAt;
    private Long profileVersion;
}
