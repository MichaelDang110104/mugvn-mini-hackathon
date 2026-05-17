package com.hackathon.backend.repositories;

import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.models.UserEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserEventRepository extends MongoRepository<UserEvent, String> {

    Optional<UserEvent> findByEventId(String eventId);

    List<UserEvent> findBySessionIdOrderByTimestampDesc(String sessionId);

    List<UserEvent> findBySessionIdAndEventTypeOrderByTimestampDesc(String sessionId, EventType eventType);
}
