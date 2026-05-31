package com.hackathon.backend.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEventMessageV1 {

    private Integer eventVersion;
    private String sessionId;
    private String userId;
    private String eventId;
    private String eventType;
    private String movieId;
    private String queryText;
    private Integer eventValue;
    private Map<String, Object> metadata;
    private String timestamp;
}
