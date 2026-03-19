package com.tokenlearn.server.controller;

import org.springframework.http.ResponseEntity;

import java.net.URI;

/**
 * Small helpers for returning direct REST representations with standard status codes.
 */
public final class RestResponses {
    private RestResponses() {
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok(body);
    }

    public static <T> ResponseEntity<T> created(URI location, T body) {
        return ResponseEntity.created(location).body(body);
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}
