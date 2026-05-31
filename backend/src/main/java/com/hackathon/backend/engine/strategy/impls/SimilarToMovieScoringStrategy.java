package com.hackathon.backend.engine.strategy.impls;

import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.strategy.ScoringStrategy;
import com.hackathon.backend.engine.utils.ScoringUtils;

import java.util.List;
import java.util.Map;

public class SimilarToMovieScoringStrategy extends ScoringStrategy {

    @Override
    protected Map<String, Double> weights() {
        return Map.of(
                "similar_to_movie", 1.0,
                "content_based",    0.3
        );
    }

    @Override
    protected List<ScoredMovie> postProcess(List<ScoredMovie> candidates, RecommendationContext ctx) {
        List<ScoredMovie> boosted = ctx.hasUserProfile()
                ? ScoringUtils.applyPersonalizationBoost(candidates, ctx.getProfile())
                : candidates;
        return sortDesc(boosted);
    }
}
