package com.hackathon.backend.services;

import com.hackathon.backend.dto.SearchResponse;
import com.hackathon.backend.dto.SearchResponse.MovieSummary;
import com.hackathon.backend.dto.SearchResponse.Reason;
import com.hackathon.backend.dto.SearchResponse.SearchItem;
import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.models.EmbeddedMovie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieSearchService {

    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;
    private final MongoTemplate mongoTemplate;

    private static final int DEFAULT_LIMIT = 10;

    public SearchResponse search(String queryText, Integer limit) {
        int effectiveLimit = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;

        if (queryText == null || queryText.isBlank()) {
            return buildColdStartResponse(effectiveLimit);
        }

        List<Double> queryEmbedding = embeddingService.embed(queryText);

        if (queryEmbedding.isEmpty()) {
            log.warn("Embedding failed for query [{}], falling back to text search", queryText);
            return buildTextFallbackResponse(queryText, effectiveLimit);
        }

        List<VectorSearchResult> results = vectorSearchService.searchByQueryVector(
                queryEmbedding, effectiveLimit);

        if (results.isEmpty()) {
            log.warn("Vector search returned empty for query [{}], falling back to text search", queryText);
            return buildTextFallbackResponse(queryText, effectiveLimit);
        }

        List<SearchItem> items = results.stream()
                .map(r -> mapToSearchItem(r, "semantic_match_to_search",
                        "Matches your search: " + queryText))
                .toList();

        return SearchResponse.builder()
                .items(items)
                .mode("semantic")
                .fallbackUsed(false)
                .query(queryText)
                .build();
    }

    public List<SearchItem> findSimilarMovies(String movieId, int limit) {
        Query query = new Query(Criteria.where("_id").is(new ObjectId(movieId)));
        query.fields().include("plot_embedding");
        EmbeddedMovie movie = mongoTemplate.findOne(query, EmbeddedMovie.class);

        if (movie == null || movie.getPlotEmbedding() == null || movie.getPlotEmbedding().isEmpty()) {
            log.warn("No embedding found for movie [{}]", movieId);
            return List.of();
        }

        List<VectorSearchResult> results = vectorSearchService.findSimilarMovies(
                movie.getPlotEmbedding(), movieId, limit);

        return results.stream()
                .map(r -> mapToSearchItem(r, "similar_to_recently_viewed",
                        "Semantically similar to the current movie"))
                .toList();
    }

    private SearchResponse buildColdStartResponse(int limit) {
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "imdb.rating"));
        query.limit(limit);
        query.fields().exclude("plot_embedding").exclude("plot_embedding_voyage_3_large");

        List<EmbeddedMovie> movies = mongoTemplate.find(query, EmbeddedMovie.class);

        List<SearchItem> items = movies.stream()
                .map(m -> SearchItem.builder()
                        .movie(toMovieSummary(m))
                        .score(m.getImdb() != null && m.getImdb().getRating() != null
                                ? m.getImdb().getRating() / 10.0 : 0.0)
                        .reasons(List.of(Reason.builder()
                                .code("trending_now")
                                .label("Popular and trending")
                                .build()))
                        .build())
                .toList();

        return SearchResponse.builder()
                .items(items)
                .mode("cold_start")
                .fallbackUsed(false)
                .query(null)
                .build();
    }

    private SearchResponse buildTextFallbackResponse(String queryText, int limit) {
        try {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage()
                    .matchingAny(queryText.split("\\s+"));

            Query query = TextQuery.queryText(textCriteria)
                    .sortByScore()
                    .limit(limit);

            query.fields().exclude("plot_embedding").exclude("plot_embedding_voyage_3_large");

            List<EmbeddedMovie> movies = mongoTemplate.find(query, EmbeddedMovie.class);

            List<SearchItem> items = movies.stream()
                    .map(m -> SearchItem.builder()
                            .movie(toMovieSummary(m))
                            .score(0.0)
                            .reasons(List.of(Reason.builder()
                                    .code("fallback_text_match")
                                    .label("Matched by text search")
                                    .build()))
                            .build())
                    .toList();

            return SearchResponse.builder()
                    .items(items)
                    .mode("fallback_text")
                    .fallbackUsed(true)
                    .query(queryText)
                    .hint("Semantic search was unavailable. Results are based on text matching.")
                    .build();
        } catch (Exception e) {
            log.error("Text fallback search also failed: {}", e.getMessage(), e);
            return SearchResponse.builder()
                    .items(List.of())
                    .mode("fallback_text")
                    .fallbackUsed(true)
                    .query(queryText)
                    .hint("Search is temporarily unavailable.")
                    .build();
        }
    }

    private SearchItem mapToSearchItem(VectorSearchResult result,
                                        String reasonCode, String reasonLabel) {
        return SearchItem.builder()
                .movie(toMovieSummary(result.getMovie()))
                .score(result.getVectorSearchScore())
                .reasons(List.of(Reason.builder()
                        .code(reasonCode)
                        .label(reasonLabel)
                        .build()))
                .build();
    }

    private MovieSummary toMovieSummary(EmbeddedMovie movie) {
        return MovieSummary.builder()
                .id(movie.getId() != null ? movie.getId().toHexString() : null)
                .title(movie.getTitle())
                .posterUrl(movie.getPoster())
                .genres(movie.getGenres())
                .ratingAvg(movie.getImdb() != null ? movie.getImdb().getRating() : null)
                .availability(SearchResponse.Availability.builder()
                        .isAvailable(true)
                        .region("global")
                        .build())
                .build();
    }
}
