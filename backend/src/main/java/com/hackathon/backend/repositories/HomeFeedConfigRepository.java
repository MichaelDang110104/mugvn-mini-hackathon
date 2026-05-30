package com.hackathon.backend.repositories;

import com.hackathon.backend.models.HomeFeedConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface HomeFeedConfigRepository extends MongoRepository<HomeFeedConfig, String> {

    // TODO: temporarily disabled — re-enable once the Redis serializer LinkedHashMap cast issue is fixed.
    // @Cacheable(value = "homeFeedConfig", key = "#audience")
    Optional<HomeFeedConfig> findByAudienceAndActiveTrue(String audience);
}
