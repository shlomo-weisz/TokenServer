package com.tokenlearn.server.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Application-specific runtime exception carrying both an HTTP status and a stable API error code.
 */
@Getter
public class AppException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public AppException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
