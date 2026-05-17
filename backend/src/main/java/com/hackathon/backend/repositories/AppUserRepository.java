package com.hackathon.backend.repositories;

import com.hackathon.backend.models.AppUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AppUserRepository extends MongoRepository<AppUser, String> {

    Optional<AppUser> findBySessionId(String sessionId);
}
