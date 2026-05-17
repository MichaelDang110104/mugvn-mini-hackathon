package com.hackathon.backend.repositories;

import com.hackathon.backend.models.EmbeddedMovie;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EmbeddedMovieRepository extends MongoRepository<EmbeddedMovie, ObjectId> {

    List<EmbeddedMovie> findByGenresContaining(String genre);

    List<EmbeddedMovie> findByTitleContainingIgnoreCase(String title);

    List<EmbeddedMovie> findByYear(Integer year);

    List<EmbeddedMovie> findByCastContaining(String actor);

    List<EmbeddedMovie> findByDirectorsContaining(String director);
}
