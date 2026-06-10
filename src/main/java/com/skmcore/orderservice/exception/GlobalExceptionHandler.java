package com.skmcore.orderservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID = "correlationId";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String cid = correlationId(request);
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        withMdc(cid, () -> log.warn("Validation failed: {} error(s)", fieldErrors.size()));
        return respond(HttpStatus.BAD_REQUEST, cid,
                new ErrorResponse(cid, 400, "Validation Failed", null, LocalDateTime.now(), fieldErrors));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        String cid = correlationId(request);
        withMdc(cid, () -> log.warn("Not found: {}", ex.getMessage()));
        return respond(HttpStatus.NOT_FOUND, cid,
                new ErrorResponse(cid, 404, "Not Found", ex.getMessage(), LocalDateTime.now(), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        String cid = correlationId(request);
        withMdc(cid, () -> log.warn("Conflict (illegal state): {}", ex.getMessage()));
        return respond(HttpStatus.CONFLICT, cid,
                new ErrorResponse(cid, 409, "Conflict", ex.getMessage(), LocalDateTime.now(), null));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        String cid = correlationId(request);
        withMdc(cid, () -> log.warn("Optimistic locking failure: {}", ex.getMessage()));
        return respond(HttpStatus.CONFLICT, cid,
                new ErrorResponse(cid, 409, "Conflict",
                        "The resource was modified concurrently — please retry the operation.",
                        LocalDateTime.now(), null));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {
        String cid = correlationId(request);
        withMdc(cid, () -> log.warn("Duplicate resource: {}", ex.getMessage()));
        return respond(HttpStatus.CONFLICT, cid,
                new ErrorResponse(cid, 409, "Conflict", ex.getMessage(), LocalDateTime.now(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        String cid = correlationId(request);
        withMdc(cid, () -> log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex));
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, cid,
                new ErrorResponse(cid, 500, "Internal Server Error",
                        "An unexpected error occurred", LocalDateTime.now(), null));
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String cid, ErrorResponse body) {
        return ResponseEntity.status(status)
                .header(HEADER_CORRELATION_ID, cid)
                .body(body);
    }

    private String correlationId(HttpServletRequest request) {
        String fromMdc = MDC.get(MDC_CORRELATION_ID);
        if (fromMdc != null && !fromMdc.isBlank()) {
            return fromMdc;
        }
        String fromHeader = request.getHeader(HEADER_CORRELATION_ID);
        return (fromHeader != null && !fromHeader.isBlank()) ? fromHeader : UUID.randomUUID().toString();
    }

    private void withMdc(String cid, Runnable action) {
        MDC.put(MDC_CORRELATION_ID, cid);
        try {
            action.run();
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
        }
    }
}
