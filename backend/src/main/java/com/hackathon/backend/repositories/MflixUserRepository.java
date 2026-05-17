package com.hackathon.backend.repositories;

import com.hackathon.backend.models.MflixUser;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MflixUserRepository extends MongoRepository<MflixUser, ObjectId> {

    Optional<MflixUser> findByEmail(String email);

    Optional<MflixUser> findByName(String name);
}
