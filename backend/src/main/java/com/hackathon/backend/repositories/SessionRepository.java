package com.hackathon.backend.repositories;

import com.hackathon.backend.models.Session;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SessionRepository extends MongoRepository<Session, ObjectId> {

    Optional<Session> findByUserId(String userId);
}
