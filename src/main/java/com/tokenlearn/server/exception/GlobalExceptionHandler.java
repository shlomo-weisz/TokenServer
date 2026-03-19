package com.tokenlearn.server.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Converts application and framework exceptions into RFC 9457 problem documents.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ProblemDetail> handleApp(AppException ex) {
        log.warn("Application error: status={}, code={}, message={}", ex.getStatus().value(), ex.getCode(), ex.getMessage());
        return asProblem(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                .toList();
        String message = errors.stream()
                .map(error -> error.get("field") + ": " + error.get("message"))
                .reduce((left, right) -> left + ", " + right)
                .orElse("Validation failed");
        return asProblem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return asProblem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An internal error occurred", null);
    }

    private ResponseEntity<ProblemDetail> asProblem(
            HttpStatus status,
            String code,
            String detail,
            List<Map<String, String>> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("urn:tokenlearn:problem:" + code.toLowerCase()));
        problem.setProperty("code", code);
        if (errors != null && !errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
