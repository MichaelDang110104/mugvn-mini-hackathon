package com.hackathon.backend.models;

import com.hackathon.backend.enums.SectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionConfig {
    private String sectionId;
    private String titleTemplate;
    private SectionType type;
    private int limit;
    private int order;
    private boolean enabled;
    private boolean dynamic;
    private Integer dynamicLimit;
}
