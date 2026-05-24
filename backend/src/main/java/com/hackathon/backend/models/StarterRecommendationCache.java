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
@Document(collection = "starter_recommendation_cache")
public class StarterRecommendationCache {
    @Id
    private String id;
    private String userId;
    private String sessionId;
    private String starterQueryText;
    private List<String> queryKeywords;
    private String querySummary;
    private String llmModel;
    private List<String> candidateMovieIds;
    private List<Double> candidateScores;
    private String retrievalMode;
    private Instant generatedAt;
    private Instant expiresAt;
    private Long profileVersionUsed;
    private Long queryVersionUsed;
}
