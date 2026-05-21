package com.hackathon.backend.engine.strategy.impls;


import com.hackathon.backend.engine.strategy.ScoringStrategy;


import java.util.Map;

public class SearchScoringStrategy extends ScoringStrategy {

    @Override
    protected Map<String, Double> weights() {
        return Map.of(
                "query_vector", 1.0,
                "user_profile_vector", 0.7
        );
    }
}
