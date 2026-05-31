package com.hackathon.backend.repositories;

import com.hackathon.backend.models.StarterRecommendationCache;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StarterRecommendationCacheRepository extends MongoRepository<StarterRecommendationCache, String> {
    Optional<StarterRecommendationCache> findTopByUserIdOrderByGeneratedAtDesc(String userId);
    Optional<StarterRecommendationCache> findTopBySessionIdOrderByGeneratedAtDesc(String sessionId);
    List<StarterRecommendationCache> findByExpiresAtBefore(Instant cutoff);
}
