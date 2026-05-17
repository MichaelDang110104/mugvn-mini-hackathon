package com.hackathon.backend.repositories;

import com.hackathon.backend.models.Movie;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MovieRepository extends MongoRepository<Movie, ObjectId> {

    List<Movie> findByGenresContaining(String genre);

    List<Movie> findByYearBetween(int startYear, int endYear);

    List<Movie> findByCastContaining(String actor);

    List<Movie> findByDirectorsContaining(String director);

    List<Movie> findByCountriesContaining(String country);

    List<Movie> findByLanguagesContaining(String language);

    List<Movie> findByRated(String rated);

    List<Movie> findByType(String type);

    Page<Movie> findByGenresContaining(String genre, Pageable pageable);

    Page<Movie> findByYearBetween(int startYear, int endYear, Pageable pageable);

    List<Movie> findByTitleContainingIgnoreCase(String title);

    List<Movie> findByImdbRatingGreaterThanEqual(Double rating);
}
