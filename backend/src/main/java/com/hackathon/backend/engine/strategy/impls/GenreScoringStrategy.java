package com.hackathon.backend.engine.strategy.impls;

import com.hackathon.backend.engine.strategy.ScoringStrategy;

import java.util.Map;

public class GenreScoringStrategy extends ScoringStrategy {

    @Override
    protected Map<String, Double> weights() {
        return Map.of(
                "genre",               1.2,
                "content_based",  0.9,
                "trending",            0.6
        );
    }
}
