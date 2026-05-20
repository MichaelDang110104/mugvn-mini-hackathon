package com.hackathon.backend.engine.entities;

import com.hackathon.backend.commons.pipeline.TaskContext;
import com.hackathon.backend.models.Movie;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@Builder
public class RecommendationContext extends TaskContext {

    private String userId;
    private String sessionId;

    private EngineMode mode;

    private String searchQuery;

    List<ScoredMovie> candidates;
    List<ScoredMovie> rankedCandidates;
    List<Movie> movieDetails;

    private List<String> recentQueries;
    private List<String> excludedMovieIds;

    public static RecommendationContext forSearch(String userId, String searchQuery, int limit) {
        return base(userId).searchQuery(searchQuery).mode(EngineMode.SEARCH).build();
    }

    private static RecommendationContext.RecommendationContextBuilder base(
            String userId) {
        return RecommendationContext.builder()
                .userId(userId)
                .excludedMovieIds(new ArrayList<>())
                .recentQueries(new ArrayList<>());
    }


    public boolean hasQuery() {
        return this.searchQuery != null && !this.searchQuery.isBlank();
    }

}
