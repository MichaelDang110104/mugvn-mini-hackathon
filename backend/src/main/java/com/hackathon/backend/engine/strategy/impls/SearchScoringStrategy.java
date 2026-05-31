package com.hackathon.backend.engine.strategy.impls;


import com.hackathon.backend.engine.strategy.ScoringStrategy;


import java.util.Map;

public class SearchScoringStrategy extends ScoringStrategy {

    @Override
    protected Map<String, Double> weights() {
        return Map.of(
                "semantic_search",     1.0,
                "keyword_search",      0.8,
                "content_based",   0.7
        );
    }
}
