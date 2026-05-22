package com.hackathon.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_profiles")
public class RecommendationProfile {

    @Id
    private String id;

    private String userId;
    private List<Double> profileEmbedding;
    private String lastComputedAt;
    private Integer sourceEventCount;
    private List<String> topGenres;
}
