package com.hackathon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StarterQueryPackage {
    private String starterQueryText;
    private List<String> queryKeywords;
    private String querySummary;
    private String llmModel;
    private long queryVersion;
}
