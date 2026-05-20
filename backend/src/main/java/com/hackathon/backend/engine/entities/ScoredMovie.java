package com.hackathon.backend.engine.entities;


import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@With
public class ScoredMovie {

    private final String movieId;
    private final double score;
    private final String source;

    private final List<String> genres;
    private final Instant releasedAt;
}