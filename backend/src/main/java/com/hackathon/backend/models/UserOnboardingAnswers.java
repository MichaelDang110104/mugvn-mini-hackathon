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
@Document(collection = "user_onboarding_answers")
public class UserOnboardingAnswers {
    @Id
    private String id;
    private String userId;
    private String sessionId;
    private List<String> selectedGenres;
    private List<String> selectedThemes;
    private List<String> favoriteTitles;
    private List<String> avoidedGenres;
    private List<String> avoidedThemes;
    private List<String> preferredLanguages;
    private String preferredEra;
    private String preferredPace;
    private String freeTextTasteSummary;
    private Instant completedAt;
    private Long version;
}
