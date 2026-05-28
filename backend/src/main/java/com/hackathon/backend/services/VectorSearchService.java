package com.hackathon.backend.services;

import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.models.EmbeddedMovie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.BinaryVector;
import org.bson.BsonBinary;
import org.bson.Document;
import org.bson.Float32BinaryVector;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private static final String VECTOR_INDEX_NAME = "vector_index";
    private static final String VECTOR_PATH = "plot_embedding";
    private static final String VECTOR_SCORE_FIELD = "vectorSearchScore";

    private final MongoTemplate mongoTemplate;
    private final EmbeddingService embeddingService;

    public List<VectorSearchResult> searchByQueryText(String queryText, int limit) {
        try {
            List<Double> queryEmbedding = embeddingService.embed(queryText);
            return searchByEmbedding(queryEmbedding, limit);
        } catch (Exception e) {
            log.error("Vector search failed for query [{}]: {}", queryText, e.getMessage(), e);
            return List.of();
        }
    }

    public List<VectorSearchResult> searchByEmbedding(List<Double> queryVector, int limit) {
        if (queryVector == null || queryVector.isEmpty()) {
            return List.of();
        }

        try {
            int effectiveLimit = limit > 0 ? limit : 10;
            int numCandidates = Math.max(effectiveLimit * 20, effectiveLimit);

            List<Document> pipeline = List.of(
                    new Document("$vectorSearch", new Document("index", VECTOR_INDEX_NAME)
                            .append("path", VECTOR_PATH)
                            .append("queryVector", toBsonFloat32Vector(queryVector))
                            .append("numCandidates", numCandidates)
                            .append("limit", effectiveLimit)),
                    new Document("$project", projectionDocument())
            );

            List<VectorSearchResult> results = new ArrayList<>();
            String collectionName = mongoTemplate.getCollectionName(EmbeddedMovie.class);
            for (Document document : mongoTemplate.getDb().getCollection(collectionName).aggregate(pipeline)) {
                results.add(mapToVectorSearchResult(document));
            }
            return results;
        } catch (Exception e) {
            log.error("Vector search failed for embedding: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<VectorSearchResult> findSimilarMovies(String moviePlot, String excludeMovieId, int limit) {
        try {
            List<Double> queryEmbedding = embeddingService.embed(moviePlot);
            return searchByEmbedding(queryEmbedding, limit + 1).stream()
                    .filter(result -> {
                        ObjectId movieId = result.getMovie().getId();
                        ObjectId excludedMovieId = parseObjectId(excludeMovieId);
                        return movieId == null || excludedMovieId == null || !movieId.equals(excludedMovieId);
                    })
                    .limit(limit)
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

    private VectorSearchResult mapToVectorSearchResult(Document document) {
        EmbeddedMovie movie = mongoTemplate.getConverter().read(EmbeddedMovie.class, document);
        Double score = document.getDouble(VECTOR_SCORE_FIELD);
        return VectorSearchResult.builder().movie(movie).vectorSearchScore(score != null ? score : 0.0).build();
    }

    private BsonBinary toBsonFloat32Vector(List<Double> queryVector) {
        Object firstValue = queryVector.getFirst();
        if (firstValue instanceof Float32BinaryVector binaryVector) {
            return new BsonBinary(binaryVector);
        }

        float[] floats = new float[queryVector.size()];
        for (int i = 0; i < queryVector.size(); i++) {
            floats[i] = queryVector.get(i).floatValue();
        }
        return new BsonBinary(BinaryVector.floatVector(floats));
    }

    private Document projectionDocument() {
        return new Document("_id", 1)
                .append("title", 1)
                .append("plot", 1)
                .append("fullplot", 1)
                .append("genres", 1)
                .append("cast", 1)
                .append("directors", 1)
                .append("writers", 1)
                .append("languages", 1)
                .append("countries", 1)
                .append("runtime", 1)
                .append("year", 1)
                .append("rated", 1)
                .append("type", 1)
                .append("poster", 1)
                .append("playbackUrl", 1)
                .append("lastupdated", 1)
                .append("released", 1)
                .append("num_mflix_comments", 1)
                .append("imdb", 1)
                .append("tomatoes", 1)
                .append("awards", 1)
                .append(VECTOR_SCORE_FIELD, new Document("$meta", "vectorSearchScore"));
    }
}
