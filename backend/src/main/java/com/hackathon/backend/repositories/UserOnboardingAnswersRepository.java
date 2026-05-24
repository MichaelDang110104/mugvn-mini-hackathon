package com.hackathon.backend.repositories;

import com.hackathon.backend.models.UserOnboardingAnswers;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserOnboardingAnswersRepository extends MongoRepository<UserOnboardingAnswers, String> {
    Optional<UserOnboardingAnswers> findTopByUserIdOrderByCompletedAtDesc(String userId);
    Optional<UserOnboardingAnswers> findTopBySessionIdOrderByCompletedAtDesc(String sessionId);
}
