package com.hackathon.backend.services;

import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.models.Movie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

        private static final Pattern LEADING_INTEGER = Pattern.compile("^[\\s]*([+-]?\\d+)");

        private final MongoTemplate mongoTemplate;

        @Value("${app.vector-search.index-name:vector_index}")
        private String vectorIndexName;

        @Value("${app.vector-search.embedding-field:plot_embedding}")
        private String embeddingField;

        @Value("${app.vector-search.default-num-candidates:200}")
        private int defaultNumCandidates;

        private static final String COLLECTION_NAME = "embedded_movies";

        public List<VectorSearchResult> searchByQueryVector(List<Double> queryEmbedding, int limit) {
                return executeVectorSearch(queryEmbedding, limit, null, null);
        }

        public List<VectorSearchResult> findSimilarMovies(List<Double> movieEmbedding,
                        String excludeMovieId,
                        int limit) {
                return executeVectorSearch(movieEmbedding, limit, excludeMovieId, null);
        }

        public List<VectorSearchResult> searchByQueryVectorWithGenreFilter(
                        List<Double> queryEmbedding, List<String> genres, int limit) {
                return executeVectorSearch(queryEmbedding, limit, null, genres);
        }

        private List<VectorSearchResult> executeVectorSearch(List<Double> queryEmbedding,
                        int limit,
                        String excludeMovieId,
                        List<String> genreFilter) {
                int numCandidates = Math.max(defaultNumCandidates, limit * 20);

                Document vectorSearchDoc = new Document()
                                .append("index", vectorIndexName)
                                .append("path", embeddingField)
                                .append("queryVector", queryEmbedding)
                                .append("numCandidates", numCandidates)
                                .append("limit", limit);

                if (genreFilter != null && !genreFilter.isEmpty()) {
                        Document filter = new Document("genres", new Document("$in", genreFilter));
                        vectorSearchDoc.append("filter", filter);
                }

                AggregationOperation vectorSearchStage = context -> new Document("$vectorSearch", vectorSearchDoc);

                AggregationOperation addScoreStage = context -> new Document("$addFields",
                                new Document("vectorSearchScore",
                                                new Document("$meta", "vectorSearchScore")));

                AggregationOperation projectStage = context -> new Document("$project",
                                new Document("plot_embedding", 0)
                                                .append("plot_embedding_voyage_3_large", 0));

                Aggregation aggregation = Aggregation.newAggregation(
                                vectorSearchStage,
                                addScoreStage,
                                projectStage);

                try {
                        AggregationResults<Document> results = mongoTemplate.aggregate(
                                        aggregation, COLLECTION_NAME, Document.class);

                        return results.getMappedResults().stream()
                                        .filter(doc -> excludeMovieId == null
                                                        || !excludeMovieId.equals(doc.getObjectId("_id").toHexString()))
                                        .map(this::mapToVectorSearchResult)
                                        .toList();
                } catch (Exception e) {
                        log.error("Vector search failed: {}", e.getMessage(), e);
                        return List.of();
                }
        }

        @SuppressWarnings("unchecked")
        private VectorSearchResult mapToVectorSearchResult(Document doc) {
                Document imdbDoc = doc.get("imdb", Document.class);
                Document tomatoesDoc = doc.get("tomatoes", Document.class);
                Document awardsDoc = doc.get("awards", Document.class);

                EmbeddedMovie movie = EmbeddedMovie.builder()
                                .id(doc.getObjectId("_id"))
                                .title(doc.getString("title"))
                                .plot(doc.getString("plot"))
                                .fullplot(doc.getString("fullplot"))
                                .genres(doc.getList("genres", String.class))
                                .cast(doc.getList("cast", String.class))
                                .directors(doc.getList("directors", String.class))
                                .writers(doc.getList("writers", String.class))
                                .languages(doc.getList("languages", String.class))
                                .countries(doc.getList("countries", String.class))
                                .runtime(doc.getInteger("runtime"))
                                .year(readInteger(doc, "year"))
                                .rated(doc.getString("rated"))
                                .type(doc.getString("type"))
                                .poster(doc.getString("poster"))
                                .lastupdated(doc.getString("lastupdated"))
                                .released(doc.getDate("released"))
                                .numMflixComments(doc.getInteger("num_mflix_comments"))
                                .imdb(mapImdb(imdbDoc))
                                .tomatoes(mapTomatoes(tomatoesDoc))
                                .awards(mapAwards(awardsDoc))
                                .build();

                double score = doc.getDouble("vectorSearchScore") != null
                                ? doc.getDouble("vectorSearchScore")
                                : 0.0;

                return VectorSearchResult.builder()
                                .movie(movie)
                                .vectorSearchScore(score)
                                .build();
        }

        private Integer readInteger(Document doc, String fieldName) {
                Object value = doc.get(fieldName);
                if (value instanceof Integer integerValue) {
                        return integerValue;
                }
                if (value instanceof Number numberValue) {
                        return numberValue.intValue();
                }
                if (value instanceof String stringValue) {
                        Matcher matcher = LEADING_INTEGER.matcher(stringValue);
                        if (matcher.find()) {
                                try {
                                        return Integer.valueOf(matcher.group(1));
                                } catch (NumberFormatException ignored) {
                                        return null;
                                }
                        }
                }
                return null;
        }

        private Movie.Imdb mapImdb(Document doc) {
                if (doc == null)
                        return null;
                return Movie.Imdb.builder()
                                .rating(doc.getDouble("rating"))
                                .votes(doc.getInteger("votes"))
                                .id(doc.getInteger("id"))
                                .build();
        }

        private Movie.Awards mapAwards(Document doc) {
                if (doc == null)
                        return null;
                return Movie.Awards.builder()
                                .wins(doc.getInteger("wins"))
                                .nominations(doc.getInteger("nominations"))
                                .text(doc.getString("text"))
                                .build();
        }

        private Movie.Tomatoes mapTomatoes(Document doc) {
                if (doc == null)
                        return null;
                return Movie.Tomatoes.builder()
                                .viewer(mapTomatoesReview(doc.get("viewer", Document.class)))
                                .critic(mapTomatoesReview(doc.get("critic", Document.class)))
                                .dvd(doc.getDate("dvd"))
                                .lastUpdated(doc.getDate("lastUpdated"))
                                .rotten(doc.getInteger("rotten"))
                                .fresh(doc.getInteger("fresh"))
                                .production(doc.getString("production"))
                                .build();
        }

        private Movie.TomatoesReview mapTomatoesReview(Document doc) {
                if (doc == null)
                        return null;
                return Movie.TomatoesReview.builder()
                                .rating(doc.getDouble("rating"))
                                .numReviews(doc.getInteger("numReviews"))
                                .meter(doc.getInteger("meter"))
                                .build();
        }
}
