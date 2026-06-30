package com.randmteam2.tripplanning.itinerary.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();

        // Check cause chain for the original message
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
            if (cause.getMessage() != null) {
                message = cause.getMessage();
                break;
            }
        }

        if (message != null && message.contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", message));
        }

        if (message != null && (message.contains("invalid") ||
                message.contains("cannot") ||
                message.contains("must") ||
                message.contains("already") ||
                message.contains("only") ||
                message.contains("exists") ||
                message.contains("active") ||
                message.contains("ACTIVE") ||
                message.contains("unavailable") ||
                message.contains("DRAFT"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", message));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
