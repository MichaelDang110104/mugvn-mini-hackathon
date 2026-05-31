package com.hackathon.backend.dto;

import com.hackathon.backend.enums.SectionType;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeFeedResponse {
    private List<HomeSection> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HomeSection {
        private String sectionId;
        private String title;
        private SectionType type;
        private List<MovieResponse> movies;
    }
}
