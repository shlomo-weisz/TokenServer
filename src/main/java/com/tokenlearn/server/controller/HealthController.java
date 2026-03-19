package com.tokenlearn.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight system status endpoint for deployments, uptime checks, and local smoke tests.
 */
@RestController
@RequestMapping("/api/system")
public class HealthController {
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
