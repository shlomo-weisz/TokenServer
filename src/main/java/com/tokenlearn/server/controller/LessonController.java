package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.ApiResponse;
import com.tokenlearn.server.dto.CreateLessonMessageRequest;
import com.tokenlearn.server.dto.UpdateLessonStateRequest;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.service.LessonService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            Authentication authentication,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String temporal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer userId = AuthUtil.requireUserId(authentication);
        if ((from == null) != (to == null)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_RANGE", "Both from and to must be provided together");
        }
        if (from != null && to != null) {
            return ok(lessonService.calendar(userId, role, status, from, to));
        }
        if ("upcoming".equalsIgnoreCase(temporal)) {
            return ok(Map.of("lessons", lessonService.upcoming(userId, role)));
        }
        if ("history".equalsIgnoreCase(temporal)) {
            List<Map<String, Object>> lessons = lessonService.history(userId, limit, offset);
            return ok(Map.of("lessons", lessons, "totalCount", lessons.size()));
        }
        throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_QUERY", "Use temporal=upcoming|history or provide from/to");
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> details(
            Authentication authentication,
            @PathVariable Integer lessonId) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.details(lessonId, userId));
    }

    @PatchMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateState(
            Authentication authentication,
            @PathVariable Integer lessonId,
            @Valid @RequestBody UpdateLessonStateRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return switch (request.getStatus().trim().toUpperCase()) {
            case "COMPLETED" -> ok(lessonService.completeLesson(lessonId, userId));
            case "CANCELLED" -> ok(lessonService.cancelLesson(lessonId, userId, request.getReason()));
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Unsupported lesson status");
        };
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
