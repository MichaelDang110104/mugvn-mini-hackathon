package com.hackathon.backend.engine.utils;

import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.models.MovieStats;

public class ObjectUtils {

    public static ScoredMovie toScoredMovie(VectorSearchResult result, String source) {
        EmbeddedMovie movie = result.getMovie();
        return fromEmbeddedMovie(movie, source, result.getVectorSearchScore());
    }

    public static ScoredMovie toScoredMovie(EmbeddedMovie movie, String source) {
        double score = (movie.getImdb() != null && movie.getImdb().getRating() != null)
                ? movie.getImdb().getRating()
                : 0.0;
        return fromEmbeddedMovie(movie, source, score);
    }

    public static ScoredMovie toScoredMovie(EmbeddedMovie movie, String source, double score) {
        return fromEmbeddedMovie(movie, source, score);
    }

    public static ScoredMovie toScoredMovie(MovieStats stats) {
        return ScoredMovie.builder()
                .movieId(stats.getMovieId())
                .score(stats.getTrendingScore())
                .source("trending")
                .genres(stats.getGenres())
                .releasedAt(stats.getReleasedAt())
                .build();
    }

    private static ScoredMovie fromEmbeddedMovie(EmbeddedMovie movie, String source, double score) {
        return ScoredMovie.builder()
                .movieId(movie.getId() != null ? movie.getId().toHexString() : null)
                .score(score)
                .source(source)
                .genres(movie.getGenres())
                .releasedAt(movie.getReleased() != null ? movie.getReleased().toInstant() : null)
                .build();
    }
}
