package com.hackathon.backend.engine.strategy.impls;

import com.hackathon.backend.engine.strategy.ScoringStrategy;

import java.util.Map;

public class RecentWatchScoringStrategy extends ScoringStrategy {

    @Override
    protected Map<String, Double> weights() {
        return Map.of("recent_watch", 1.0);
    }
}
