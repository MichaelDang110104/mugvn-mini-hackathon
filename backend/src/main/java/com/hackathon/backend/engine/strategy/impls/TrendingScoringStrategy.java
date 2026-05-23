package com.hackathon.backend.engine.strategy.impls;

import com.hackathon.backend.engine.strategy.ScoringStrategy;

import java.util.Map;

public class TrendingScoringStrategy extends ScoringStrategy {

    @Override
    protected Map<String, Double> weights() {
        return Map.of("trending", 1.0);
    }
}
