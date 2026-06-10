package com.skmcore.orderservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String correlationId,
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        List<FieldError> errors
) {
    public record FieldError(String field, String message) {}
}
