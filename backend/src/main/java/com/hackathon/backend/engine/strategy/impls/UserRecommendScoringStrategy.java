package com.hackathon.backend.engine.strategy.impls;

import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.strategy.ScoringStrategy;
import com.hackathon.backend.engine.utils.ScoringUtils;

import java.util.List;
import java.util.Map;

public class UserRecommendScoringStrategy extends ScoringStrategy {

    @Override
    protected Map<String, Double> weights() {
        return Map.of(
                "content_based", 0.5,
                "genre",         0.3,
                "trending",      0.2
        );
    }

    @Override
    protected List<ScoredMovie> postProcess(List<ScoredMovie> candidates, RecommendationContext ctx) {
        return sortDesc(ScoringUtils.applyFreshness(ScoringUtils.applyDiversityPenalty(candidates)));
    }
}
