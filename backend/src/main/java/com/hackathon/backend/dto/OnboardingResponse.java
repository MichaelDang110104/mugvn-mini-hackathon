package com.hackathon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingResponse {
    private String sessionId;
    private boolean completed;
    private long profileVersion;
}
