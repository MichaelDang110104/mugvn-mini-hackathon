package com.hackathon.backend.engine.utils;

import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.models.EmbeddedMovie;

public class ObjectUtils {

    public static ScoredMovie toScoredMovie(VectorSearchResult result, String source) {
        EmbeddedMovie movie = result.getMovie();
        return ScoredMovie.builder()
                .movieId(movie.getId() != null ? movie.getId().toHexString() : null)
                .score(result.getVectorSearchScore())
                .source(source)
                .genres(movie.getGenres())
                .releasedAt(movie.getReleased() != null ? movie.getReleased().toInstant() : null)
                .build();
    }
}
