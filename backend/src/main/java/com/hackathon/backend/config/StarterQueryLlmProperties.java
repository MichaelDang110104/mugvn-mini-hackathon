package com.hackathon.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recommendation.starter-query.llm")
public record StarterQueryLlmProperties(
        boolean enabled,
        String model,
        double temperature
) {
}
