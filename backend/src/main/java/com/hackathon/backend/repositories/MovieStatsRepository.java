package com.hackathon.backend.repositories;

import com.hackathon.backend.models.MovieStats;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MovieStatsRepository extends MongoRepository<MovieStats, String> {
}
