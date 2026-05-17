package com.hackathon.backend.services;

import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.models.Movie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private final VectorStore vectorStore;

    public List<VectorSearchResult> searchByQueryText(String queryText, int limit) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(queryText)
                    .topK(limit)
                    .build();
            return vectorStore.similaritySearch(request).stream()
                    .map(this::mapToVectorSearchResult)
                    .toList();
        } catch (Exception e) {
            log.error("Vector search failed for query [{}]: {}", queryText, e.getMessage(), e);
            return List.of();
        }
    }

    public List<VectorSearchResult> findSimilarMovies(String moviePlot, String excludeMovieId, int limit) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(moviePlot)
                    .topK(limit + 1)
                    .build();
            return vectorStore.similaritySearch(request).stream()
                    .filter(doc -> {
                        ObjectId docId = parseObjectId(doc.getId());
                        ObjectId excludeId = parseObjectId(excludeMovieId);
                        return docId == null || excludeId == null || !docId.equals(excludeId);
                    })
                    .limit(limit)
                    .map(this::mapToVectorSearchResult)
                    .toList();
        } catch (Exception e) {
            log.error("Similar movie search failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private ObjectId parseObjectId(String id) {
        if (id == null) return null;
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            log.warn("Document ID [{}] is not a valid ObjectId", id);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private VectorSearchResult mapToVectorSearchResult(Document doc) {
        Map<String, Object> meta = doc.getMetadata();

        EmbeddedMovie movie = EmbeddedMovie.builder()
                .id(parseObjectId(doc.getId()))
                .title((String) meta.get("title"))
                .plot((String) meta.get("plot"))
                .fullplot((String) meta.get("fullplot"))
                .genres((List<String>) meta.get("genres"))
                .cast((List<String>) meta.get("cast"))
                .directors((List<String>) meta.get("directors"))
                .writers((List<String>) meta.get("writers"))
                .languages((List<String>) meta.get("languages"))
                .countries((List<String>) meta.get("countries"))
                .runtime(toInteger(meta.get("runtime")))
                .year(toInteger(meta.get("year")))
                .rated((String) meta.get("rated"))
                .type((String) meta.get("type"))
                .poster((String) meta.get("poster"))
                .lastupdated((String) meta.get("lastupdated"))
                .released(toDate(meta.get("released")))
                .numMflixComments(toInteger(meta.get("num_mflix_comments")))
                .imdb(mapImdb(meta.get("imdb")))
                .tomatoes(mapTomatoes(meta.get("tomatoes")))
                .awards(mapAwards(meta.get("awards")))
                .build();

        double score = doc.getScore() != null ? doc.getScore() : 0.0;
        return VectorSearchResult.builder().movie(movie).vectorSearchScore(score).build();
    }

    @SuppressWarnings("unchecked")
    private Movie.Imdb mapImdb(Object obj) {
        if (obj == null) return null;
        Map<String, Object> doc = (Map<String, Object>) obj;
        return Movie.Imdb.builder()
                .rating(toDouble(doc.get("rating")))
                .votes(toInteger(doc.get("votes")))
                .id(toInteger(doc.get("id")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Movie.Awards mapAwards(Object obj) {
        if (obj == null) return null;
        Map<String, Object> doc = (Map<String, Object>) obj;
        return Movie.Awards.builder()
                .wins(toInteger(doc.get("wins")))
                .nominations(toInteger(doc.get("nominations")))
                .text((String) doc.get("text"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Movie.Tomatoes mapTomatoes(Object obj) {
        if (obj == null) return null;
        Map<String, Object> doc = (Map<String, Object>) obj;
        return Movie.Tomatoes.builder()
                .viewer(mapTomatoesReview(doc.get("viewer")))
                .critic(mapTomatoesReview(doc.get("critic")))
                .dvd(toDate(doc.get("dvd")))
                .lastUpdated(toDate(doc.get("lastUpdated")))
                .rotten(toInteger(doc.get("rotten")))
                .fresh(toInteger(doc.get("fresh")))
                .production((String) doc.get("production"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Movie.TomatoesReview mapTomatoesReview(Object obj) {
        if (obj == null) return null;
        Map<String, Object> doc = (Map<String, Object>) obj;
        return Movie.TomatoesReview.builder()
                .rating(toDouble(doc.get("rating")))
                .numReviews(toInteger(doc.get("numReviews")))
                .meter(toInteger(doc.get("meter")))
                .build();
    }

    private Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Long l) return l.intValue();
        if (obj instanceof Double d) return d.intValue();
        return null;
    }

    private Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Double d) return d;
        if (obj instanceof Integer i) return i.doubleValue();
        if (obj instanceof Long l) return l.doubleValue();
        return null;
    }

    private Date toDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Date d) return d;
        if (obj instanceof Long l) return new Date(l);
        if (obj instanceof Instant instant) return Date.from(instant);
        return null;
    }
}
