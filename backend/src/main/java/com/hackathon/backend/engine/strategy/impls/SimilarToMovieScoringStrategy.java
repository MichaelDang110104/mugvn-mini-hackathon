package com.hackathon.backend.engine.strategy.impls;

import com.hackathon.backend.engine.strategy.ScoringStrategy;

import java.util.Map;

public class SimilarToMovieScoringStrategy extends ScoringStrategy {

    @Override
    protected Map<String, Double> weights() {
        return Map.of(
                "similar_to_movie",    1.2,
                "user_profile_vector", 0.6
        );
    }
}
