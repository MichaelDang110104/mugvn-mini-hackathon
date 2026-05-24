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
    private String movieId;
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

    public static RecommendationContext forTrending(String userId, int limit) {
        return base(userId).limit(limit).mode(EngineMode.TRENDING).build();
    }

    public static RecommendationContext forGenre(String userId, int limit) {
        return base(userId).limit(limit).mode(EngineMode.GENRE).build();
    }

    public static RecommendationContext forRecentWatch(String userId, int limit) {
        return base(userId).limit(limit).mode(EngineMode.RECENT_WATCH).build();
    }

    /**
     * Use when user choose specific genre
     * @param userId
     * @param genre
     * @param limit
     * @return
     */
    public static RecommendationContext forGenre(String userId, String genre, int limit) {
        return base(userId).genre(genre).limit(limit).mode(EngineMode.GENRE).build();
    }

    /**
     * Use when user choose specific movie
     * @param userId
     * @param movieId
     * @param limit
     * @return
     */
    public static RecommendationContext forSimilarToMovie(String userId, String movieId, int limit) {
        return base(userId).movieId(movieId).limit(limit).mode(EngineMode.SIMILAR_TO_MOVIE).build();
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
