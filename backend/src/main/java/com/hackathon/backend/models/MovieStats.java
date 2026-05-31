package com.hackathon.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "movie_stats")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieStats {
    @Id
    @Indexed(unique = true)
    private String movieId;

    private long viewCount;
    private long likeCount;
    private long watchCompleteCount;

    private long viewCount7d;
    private long likeCount7d;
    private long watchCompleteCount7d;

    private double wilsonScore;
    private double decayScore;
    private double trendingScore;

    private List<String> genres;
    private Instant releasedAt;
    private Instant firstTrendingAt;
}
