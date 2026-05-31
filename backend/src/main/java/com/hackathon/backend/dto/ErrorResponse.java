package com.hackathon.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private ErrorBody error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorBody {
        private String code;
        private String message;
        private List<FieldError> details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String reason;
    }

    public static ErrorResponse validationError(String message, List<FieldError> details) {
        return ErrorResponse.builder()
                .error(ErrorBody.builder()
                        .code("VALIDATION_ERROR")
                        .message(message)
                        .details(details)
                        .build())
                .build();
    }

    public static ErrorResponse notFound(String message) {
        return ErrorResponse.builder()
                .error(ErrorBody.builder()
                        .code("NOT_FOUND")
                        .message(message)
                        .build())
                .build();
    }

    public static ErrorResponse internalError(String message) {
        return ErrorResponse.builder()
                .error(ErrorBody.builder()
                        .code("INTERNAL_ERROR")
                        .message(message)
                        .build())
                .build();
    }
}
