package com.hackathon.backend.engine.strategy;

import com.hackathon.backend.engine.entities.EngineMode;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.strategy.impls.GenreScoringStrategy;
import com.hackathon.backend.engine.strategy.impls.SearchScoringStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ScoringStrategyRegistry {

    private final Map<EngineMode, ScoringStrategy> strategies = Map.of(
            EngineMode.SEARCH, new SearchScoringStrategy(),
            EngineMode.GENRE,  new GenreScoringStrategy()
    );

    private static final ScoringStrategy defaultStrategy = new ScoringStrategy() {
        @Override
        protected Map<String, Double> weights() {
            return Map.of(); // fallback — không biết source nào
        }

        @Override
        protected List<ScoredMovie> postProcess(List<ScoredMovie> candidates,
                                                RecommendationContext ctx) {
            return sortDesc(candidates);
        }
    };

    public ScoringStrategy get(EngineMode mode) {
        return strategies.getOrDefault(mode, defaultStrategy);
    }
}