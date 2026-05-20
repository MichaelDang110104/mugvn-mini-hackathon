package com.hackathon.backend.engine.utils;

import com.hackathon.backend.engine.entities.ScoredMovie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScoringUtils {
    public static List<ScoredMovie> aggregate(List<ScoredMovie> candidates) {
        Map<String, ScoredMovie> map = new LinkedHashMap<>();

        for (ScoredMovie movie : candidates) {
            map.merge(movie.getMovieId(), movie, (existing, incoming) ->
                    existing.withScore(
                            existing.getScore() + incoming.getScore() * 0.5
                    )
            );
        }

        return new ArrayList<>(map.values());
    }
}
