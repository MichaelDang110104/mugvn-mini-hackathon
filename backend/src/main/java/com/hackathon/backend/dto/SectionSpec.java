package com.hackathon.backend.dto;

import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.enums.SectionType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionSpec {
    private String sectionId;
    private String title;
    private SectionType type;
    private RecommendationContext context;
}
