package com.hackathon.backend.services;

import com.hackathon.backend.dto.OnboardingMovieOptionResponse;
import com.hackathon.backend.dto.OnboardingMovieOptionResponse.MovieOption;
import com.hackathon.backend.dto.OnboardingOptionsResponse;
import com.hackathon.backend.models.EmbeddedMovie;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingCatalogService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private static final String COLLECTION = "embedded_movies";

    private final MongoTemplate mongoTemplate;

    public OnboardingOptionsResponse getOptions() {
        List<String> genres = mongoTemplate.findDistinct(new Query(), "genres", COLLECTION, String.class)
                .stream()
                .filter(genre -> genre != null && !genre.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();

        return OnboardingOptionsResponse.builder()
                .genres(genres)
                .build();
    }

    public OnboardingMovieOptionResponse getMovieOptions(String queryText, List<String> genres, Integer limit) {
        int effectiveLimit = normalizeLimit(limit);
        List<MovieOption> movies = hasText(queryText)
                ? searchByTitle(queryText.trim(), effectiveLimit)
                : sampleByGenres(genres, effectiveLimit);

        return OnboardingMovieOptionResponse.builder()
                .movies(movies)
                .build();
    }

    private List<MovieOption> searchByTitle(String queryText, int limit) {
        Query query = new Query(Criteria.where("title").regex(queryText, "i"));
        query.limit(limit);
        query.with(Sort.by(Sort.Direction.ASC, "title"));
        query.fields().include("title", "genres", "poster", "year");

        return mongoTemplate.find(query, EmbeddedMovie.class).stream()
                .map(this::toMovieOption)
                .toList();
    }

    private List<MovieOption> sampleByGenres(List<String> genres, int limit) {
        if (genres == null || genres.isEmpty()) {
            return List.of();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("genres").in(genres)),
                Aggregation.sample(limit),
                Aggregation.project("title", "genres", "poster", "year"));

        AggregationResults<EmbeddedMovie> results = mongoTemplate.aggregate(aggregation, COLLECTION, EmbeddedMovie.class);
        return results.getMappedResults().stream()
                .sorted(Comparator.comparing(movie -> movie.getTitle() == null ? "" : movie.getTitle(), String.CASE_INSENSITIVE_ORDER))
                .map(this::toMovieOption)
                .toList();
    }

    private MovieOption toMovieOption(EmbeddedMovie movie) {
        return MovieOption.builder()
                .movieId(movie.getId() != null ? movie.getId().toHexString() : null)
                .title(movie.getTitle())
                .genres(movie.getGenres())
                .posterUrl(movie.getPoster())
                .year(movie.getYear())
                .build();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
