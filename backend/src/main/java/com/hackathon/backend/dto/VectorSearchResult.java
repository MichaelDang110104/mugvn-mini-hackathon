package com.hackathon.backend.dto;

import com.hackathon.backend.models.EmbeddedMovie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchResult {

    private EmbeddedMovie movie;
    private double vectorSearchScore;
}
