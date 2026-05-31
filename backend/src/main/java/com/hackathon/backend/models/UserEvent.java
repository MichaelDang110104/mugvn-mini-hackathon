package com.hackathon.backend.models;

import com.hackathon.backend.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_events")
@CompoundIndex(name = "idx_session_event", def = "{'sessionId': 1, 'eventType': 1, 'movieId': 1}")
public class UserEvent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    private String userId;
    private String sessionId;
    private EventType eventType;
    private String movieId;
    private String queryText;
    private Integer eventValue;
    private Map<String, Object> metadata;
    private Instant timestamp;
}
