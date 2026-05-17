package com.hackathon.backend.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum EventType {

    VIEW("view", 1),
    CLICK("click", 2),
    SEARCH("search", 2),
    LIKE("like", 4),
    SAVE("save", 4),
    WATCH_START("watch_start", 5),
    RATING("rate", 5);

    private final String value;
    private final int weight;

    EventType(String value, int weight) {
        this.value = value;
        this.weight = weight;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public int getWeight() {
        return weight;
    }

    public static EventType fromValue(String value) {
        if ("rating".equals(value)) {
            return RATING;
        }
        return Arrays.stream(values())
                .filter(t -> t.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown event type: " + value));
    }
}
