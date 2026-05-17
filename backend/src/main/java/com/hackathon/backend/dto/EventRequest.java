package com.hackathon.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventRequest {

    @NotBlank(message = "sessionId is required")
    private String sessionId;

    @NotBlank(message = "eventId is required")
    private String eventId;

    @NotBlank(message = "eventType is required")
    private String eventType;

    private String movieId;

    private String queryText;

    @Min(value = 1, message = "eventValue must be between 1 and 5 for rate events")
    @Max(value = 5, message = "eventValue must be between 1 and 5 for rate events")
    private Integer eventValue;

    private Map<String, Object> metadata;

    private String timestamp;
}
