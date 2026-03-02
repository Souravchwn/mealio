package com.mealio.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 ───────────────────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
            HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    // ── 409 cut-off ───────────────────────────────────────────────────────────
    @ExceptionHandler(CutOffTimeExceededException.class)
    public ResponseEntity<ErrorResponse> handleCutOff(CutOffTimeExceededException ex,
            HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    // ── 409 month closed ──────────────────────────────────────────────────────
    @ExceptionHandler(MonthAlreadyClosedException.class)
    public ResponseEntity<ErrorResponse> handleMonthClosed(MonthAlreadyClosedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    // ── 409 optimistic lock ──────────────────────────────────────────────
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {
        // Retries are already exhausted at this point (3 attempts by @Retryable)
        return build(HttpStatus.CONFLICT,
                "Data was modified by another request. Please retry.",
                request.getRequestURI());
    }

    // ── 503 lock timeout (DB pessimistic lock waited too long) ──────────────
    @ExceptionHandler(CannotAcquireLockException.class)
    public ResponseEntity<ErrorResponse> handleLockTimeout(CannotAcquireLockException ex,
            HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE,
                "Server is under heavy load. Please retry in a few seconds.",
                request.getRequestURI());
    }

    // ── 400 validation ────────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
            fieldErrors.put(field, error.getDefaultMessage());
        });
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                request.getRequestURI(),
                Instant.now(),
                fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── 500 catch-all ─────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex,
            HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage(),
                request.getRequestURI());
    }

    // ── helper ────────────────────────────────────────────────────────────────
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message, path, Instant.now(), null));
    }

    // ── inner record ──────────────────────────────────────────────────────────
    public record ErrorResponse(
            int status,
            String message,
            String path,
            Instant timestamp,
            Map<String, String> fieldErrors) {
    }
}
