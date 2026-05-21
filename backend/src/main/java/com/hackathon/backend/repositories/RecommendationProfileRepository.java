package com.hackathon.backend.repositories;

import com.hackathon.backend.models.RecommendationProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecommendationProfileRepository extends MongoRepository<RecommendationProfile, String> {
}
