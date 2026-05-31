package com.hackathon.backend.repositories;

import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.models.UserEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserEventRepository extends MongoRepository<UserEvent, String> {

    Optional<UserEvent> findByEventId(String eventId);

    List<UserEvent> findBySessionIdOrderByTimestampDesc(String sessionId);

    List<UserEvent> findBySessionIdAndEventTypeOrderByTimestampDesc(String sessionId, EventType eventType);

    List<UserEvent> findByUserIdOrderByTimestampDesc(String userId);

    @Query(value = "{ 'userId': ?0, 'movieId': { $exists: true, $ne: null }, 'eventType': { $in: ?1 } }", fields = "{ 'movieId': 1, '_id': 0 }")
    List<UserEvent> findSeenMovieEventsByUserId(String userId, List<EventType> eventTypes);

    List<UserEvent> findByUserIdAndEventTypeOrderByTimestampDesc(String userId, EventType eventType, Pageable pageable);
}
