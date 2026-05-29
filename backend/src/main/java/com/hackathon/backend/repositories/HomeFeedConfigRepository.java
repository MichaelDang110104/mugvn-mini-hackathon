package com.hackathon.backend.repositories;

import com.hackathon.backend.models.HomeFeedConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface HomeFeedConfigRepository extends MongoRepository<HomeFeedConfig, String> {
    Optional<HomeFeedConfig> findByAudienceAndActiveTrue(String audience);
}
