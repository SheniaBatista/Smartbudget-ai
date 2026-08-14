package com.smartbudget.infrastructure.web.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Instant timestamp,
                       int status,
                       String error,
                       String message,
                       List<FieldViolation> details) {
    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, null);
    }

    public static ApiError of(int status, String error, String message, List<FieldViolation> details) {
        return new ApiError(Instant.now(), status, error, message,
                details == null || details.isEmpty() ? null : details);
    }
}
