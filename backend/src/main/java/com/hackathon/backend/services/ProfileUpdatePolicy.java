package com.hackathon.backend.services;

import com.hackathon.backend.enums.EventType;
import org.springframework.stereotype.Service;

@Service
public class ProfileUpdatePolicy {

    public boolean shouldRecompute(EventType eventType, Integer eventValue) {
        if (eventType == EventType.LIKE || eventType == EventType.SAVE || eventType == EventType.WATCH_START) {
            return true;
        }

        return eventType == EventType.RATING && eventValue != null && eventValue >= 4;
    }
}
