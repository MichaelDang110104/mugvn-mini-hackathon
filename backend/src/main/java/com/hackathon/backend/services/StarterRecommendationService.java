package com.hackathon.backend.services;

import com.hackathon.backend.dto.StarterQueryPackage;
import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.models.RecommendationProfile;
import com.hackathon.backend.models.StarterRecommendationCache;
import com.hackathon.backend.repositories.StarterRecommendationCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StarterRecommendationService {

    private final StarterRecommendationCacheRepository cacheRepository;
    private final VectorSearchService vectorSearchService;
    private final StarterQueryLlmService starterQueryLlmService;

    public StarterRecommendationCache refreshStarterCandidates(RecommendationProfile profile, String freeText) {
        StarterQueryPackage queryPackage = starterQueryLlmService.buildForProfile(profile, freeText);
        List<VectorSearchResult> results = vectorSearchService.searchByQueryText(queryPackage.getStarterQueryText(), 20);
        Instant now = Instant.now();

        StarterRecommendationCache cache = StarterRecommendationCache.builder()
                .userId(profile.getUserId())
                .sessionId(profile.getSessionId())
                .starterQueryText(queryPackage.getStarterQueryText())
                .queryKeywords(queryPackage.getQueryKeywords())
                .querySummary(queryPackage.getQuerySummary())
                .llmModel(queryPackage.getLlmModel())
                .candidateMovieIds(results.stream().map(r -> r.getMovie().getId().toHexString()).toList())
                .candidateScores(results.stream().map(VectorSearchResult::getVectorSearchScore).toList())
                .retrievalMode("onboarding_starter")
                .generatedAt(now)
                .expiresAt(now.plusSeconds(86400))
                .profileVersionUsed(profile.getProfileVersion())
                .queryVersionUsed(queryPackage.getQueryVersion())
                .build();

        return cacheRepository.save(cache);
    }
}
