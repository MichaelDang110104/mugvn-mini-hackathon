package com.hackathon.backend.engine.entities;

import com.hackathon.backend.commons.pipeline.TaskContext;
import com.hackathon.backend.models.Movie;
import com.hackathon.backend.models.RecommendationProfile;
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
    private String genre;
    private List<Double> userProfileEmbedding;
    private RecommendationProfile profile;
    private int limit;

    List<ScoredMovie> candidates;
    List<ScoredMovie> rankedCandidates;
    List<Movie> movieDetails;

    private List<String> recentQueries;
    private List<String> excludedMovieIds;

    public static RecommendationContext forSearch(String userId, String searchQuery, int limit) {
        return base(userId).searchQuery(searchQuery).limit(limit).mode(EngineMode.SEARCH).build();
    }

    private static RecommendationContext.RecommendationContextBuilder base(
            String userId) {
        return RecommendationContext.builder()
                .userId(userId)
                .excludedMovieIds(new ArrayList<>())
                .recentQueries(new ArrayList<>());
    }


    public boolean hasQuery() {
        return this.searchQuery == null || this.searchQuery.isBlank();
    }

    public synchronized void addCandidates(List<ScoredMovie> movies) {
        if (candidates == null) {
            candidates = new ArrayList<>();
        }
        candidates.addAll(movies);
    }

    public void putCandidateGroup(String key, List<ScoredMovie> movies) {
        set("candidateGroup:" + key, movies);
    }

    public List<ScoredMovie> getCandidateGroup(String key) {
        List<ScoredMovie> movies = get("candidateGroup:" + key);
        return movies != null ? movies : List.of();
    }

}
