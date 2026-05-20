package com.hackathon.backend.engine.strategy.impls;


import com.hackathon.backend.engine.strategy.ScoringStrategy;


import java.util.Map;

public class SearchScoringStrategy extends ScoringStrategy {

    @Override
    protected Map<String, Double> weights() {
        return Map.of(
                "text_search",   1.0
        );
    }
}
