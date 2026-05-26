package com.hackathon.backend.kafka;

import com.hackathon.backend.dto.EventRequest;

public interface UserEventPublisher {

    void publish(EventRequest request);
}
