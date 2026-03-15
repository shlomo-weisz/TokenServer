package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.ApiResponse;
import com.tokenlearn.server.dto.CancelLessonRequest;
import com.tokenlearn.server.dto.CreateLessonMessageRequest;
import com.tokenlearn.server.dto.RateLessonRequest;
import com.tokenlearn.server.service.LessonService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.tokenlearn.server.controller.ApiResponses.ok;

/**
 * Endpoints for the scheduled lesson lifecycle, including completion, cancellation, ratings, and lesson messaging.
 */
@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> upcoming(
            Authentication authentication,
            @RequestParam(required = false) String role) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.upcoming(userId, role));
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> details(
            Authentication authentication,
            @PathVariable Integer lessonId) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.details(lessonId, userId));
    }

    @PutMapping("/{lessonId}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> complete(
            Authentication authentication,
            @PathVariable Integer lessonId,
            @RequestBody(required = false) Map<String, Object> metadata) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.completeLesson(lessonId, userId));
    }

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancel(
            Authentication authentication,
            @PathVariable Integer lessonId,
            @Valid @RequestBody(required = false) CancelLessonRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        String reason = request == null ? null : request.getReason();
        return ok(lessonService.cancelLesson(lessonId, userId, reason));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> history(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.history(userId, limit, offset));
    }

    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calendar(
            Authentication authentication,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.calendar(userId, role, status, from, to));
    }

    @PostMapping("/{lessonId}/rate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rate(
            Authentication authentication,
            @PathVariable Integer lessonId,
            @Valid @RequestBody RateLessonRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.rateLesson(lessonId, userId, request));
    }

    @PutMapping("/{lessonId}/rate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRate(
            Authentication authentication,
            @PathVariable Integer lessonId,
            @Valid @RequestBody RateLessonRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.updateLessonRating(lessonId, userId, request));
    }

    @PostMapping("/{lessonId}/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            Authentication authentication,
            @PathVariable Integer lessonId,
            @Valid @RequestBody CreateLessonMessageRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.sendLessonMessage(lessonId, userId, request));
    }
}
