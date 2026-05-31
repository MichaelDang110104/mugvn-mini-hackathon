package com.hackathon.backend.repositories;

import com.hackathon.backend.models.RecommendationProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RecommendationProfileRepository extends MongoRepository<RecommendationProfile, String> {
    Optional<RecommendationProfile> findByUserId(String userId);
    Optional<RecommendationProfile> findBySessionId(String sessionId);
}
