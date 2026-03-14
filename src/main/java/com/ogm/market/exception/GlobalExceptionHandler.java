package com.ogm.market.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts all thrown exceptions into consistent JSON error responses.
 *
 * Without this, Spring returns HTML error pages or noisy stack traces
 * to the frontend. With it, every error looks like:
 *   {"error": "Property not found for slug: xyz"}
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── @Valid validation failures ──────────────────────────────────────────
    // Fired when a @RequestBody field fails a constraint like @NotBlank.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    // ── ResponseStatusException (our explicit throws) ───────────────────────
    // Fired by requireAdmin(), getPropertyBySlug(), processBrochureRequest()
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(
            ResponseStatusException ex) {

        if (ex.getStatusCode().value() >= 500) {
            log.error("Server error: {}", ex.getReason(), ex);
        }

        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() != null
                        ? ex.getReason()
                        : ex.getMessage()));
    }

    // ── Catch-all ───────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred. Please try again."));
    }
}