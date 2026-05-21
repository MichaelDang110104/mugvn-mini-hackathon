package com.hackathon.backend.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum EventType {

    VIEW("view", 0.1),
    CLICK("click", 0.2),
    SEARCH("search", 0.2),
    LIKE("like", 1.0),
    SAVE("save", 0.8),
    WATCH_START("watch_start", 0.5),
    RATING("rating", 1.0);

    private final String value;
    private final double weight;

    EventType(String value, double weight) {
        this.value = value;
        this.weight = weight;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public double getWeight() {
        return weight;
    }

    public static EventType fromValue(String value) {
        if ("rate".equals(value)) {
            return RATING;
        }

        return Arrays.stream(values())
                .filter(t -> t.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown event type: " + value));
    }
}
