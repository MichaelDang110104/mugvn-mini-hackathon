package com.hackathon.backend.engine.strategy;

import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.utils.ScoringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public abstract class ScoringStrategy {

    protected abstract Map<String, Double> weights();

    public List<ScoredMovie> score(List<ScoredMovie> candidates,
                                   RecommendationContext ctx) {
        List<ScoredMovie> weighted   = applyWeights(candidates);
        List<ScoredMovie> aggregated = ScoringUtils.aggregate(weighted);
        return postProcess(aggregated, ctx);
    }

    protected List<ScoredMovie> postProcess(List<ScoredMovie> candidates,
                                            RecommendationContext ctx) {
        return sortDesc(candidates);
    }

    private List<ScoredMovie> applyWeights(List<ScoredMovie> candidates) {
        return candidates.stream()
                .map(sm -> sm.withScore(
                        sm.getScore() * weights().getOrDefault(sm.getSource(), 0.1)
                ))
                .toList();
    }

    protected List<ScoredMovie> sortDesc(List<ScoredMovie> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(ScoredMovie::getScore).reversed())
                .toList();
    }
}
