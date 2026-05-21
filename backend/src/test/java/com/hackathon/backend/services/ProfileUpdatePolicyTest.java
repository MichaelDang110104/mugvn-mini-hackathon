package com.hackathon.backend.services;

import com.hackathon.backend.enums.EventType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileUpdatePolicyTest {

    private final ProfileUpdatePolicy policy = new ProfileUpdatePolicy();

    @Test
    void shouldRecompute_returnsTrueForLikeSaveAndWatchStart() {
        assertThat(policy.shouldRecompute(EventType.LIKE, null)).isTrue();
        assertThat(policy.shouldRecompute(EventType.SAVE, null)).isTrue();
        assertThat(policy.shouldRecompute(EventType.WATCH_START, null)).isTrue();
    }

    @Test
    void shouldRecompute_returnsTrueForHighRatingsOnly() {
        assertThat(policy.shouldRecompute(EventType.RATING, 4)).isTrue();
        assertThat(policy.shouldRecompute(EventType.RATING, 5)).isTrue();
        assertThat(policy.shouldRecompute(EventType.RATING, 3)).isFalse();
        assertThat(policy.shouldRecompute(EventType.RATING, null)).isFalse();
    }

    @Test
    void shouldRecompute_returnsFalseForWeakSignals() {
        assertThat(policy.shouldRecompute(EventType.VIEW, null)).isFalse();
        assertThat(policy.shouldRecompute(EventType.CLICK, null)).isFalse();
        assertThat(policy.shouldRecompute(EventType.SEARCH, null)).isFalse();
    }
}
