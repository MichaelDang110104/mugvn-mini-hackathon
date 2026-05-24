package com.hackathon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingRequest {
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
}
