package com.hackathon.backend.repositories;

import com.hackathon.backend.models.EmbeddedMovie;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface EmbeddedMovieRepository extends MongoRepository<EmbeddedMovie, ObjectId> {

    List<EmbeddedMovie> findByGenresContaining(String genre);

    List<EmbeddedMovie> findByGenresContaining(String genre, Pageable pageable);

    @Query("{ 'genres': { $in: ?0 } }")
    List<EmbeddedMovie> findByGenresIn(List<String> genres, Pageable pageable);

    List<EmbeddedMovie> findByTitleContainingIgnoreCase(String title);

    List<EmbeddedMovie> findByYear(Integer year);

    List<EmbeddedMovie> findByCastContaining(String actor);

    List<EmbeddedMovie> findByDirectorsContaining(String director);
}
