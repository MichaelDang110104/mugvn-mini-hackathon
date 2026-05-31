package com.hackathon.backend.services;

import com.hackathon.backend.models.EmbeddedMovie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieEmbeddingService {

    private final EmbeddingService embeddingService;
    private final MongoTemplate mongoTemplate;

    public void embedMovie(String movieId) {
        EmbeddedMovie movie = mongoTemplate.findById(new ObjectId(movieId), EmbeddedMovie.class);
        if (movie == null) {
            log.warn("Movie [{}] not found for embedding", movieId);
            return;
        }

        String text = embeddingService.buildMovieEmbeddingText(
                movie.getTitle(), movie.getGenres(), null, movie.getPlot());

        List<Double> embedding = embeddingService.embed(text);
        if (embedding.isEmpty()) {
            log.error("Failed to generate embedding for movie [{}]", movieId);
            return;
        }

        Update update = new Update().set("plot_embedding", embedding);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(new ObjectId(movieId))),
                update,
                EmbeddedMovie.class);

        log.info("Updated plot_embedding for movie [{}] (dim={})", movieId, embedding.size());
    }

    public int embedAllMovies() {
        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("plot_embedding").exists(false),
                Criteria.where("plot_embedding").is(null)));
        query.fields().include("_id", "title", "genres", "plot");

        List<EmbeddedMovie> movies = mongoTemplate.find(query, EmbeddedMovie.class);
        log.info("Found {} movies without plot_embedding", movies.size());

        int success = 0;
        for (EmbeddedMovie movie : movies) {
            try {
                embedMovie(movie.getId().toHexString());
                success++;
            } catch (Exception e) {
                log.error("Failed to embed movie [{}]: {}", movie.getId(), e.getMessage());
            }
        }

        log.info("Embedded {}/{} movies successfully", success, movies.size());
        return success;
    }
}
