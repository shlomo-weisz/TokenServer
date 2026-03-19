package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.CreateLessonMessageRequest;
import com.tokenlearn.server.dto.UpdateLessonStateRequest;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.service.AdminService;
import com.tokenlearn.server.service.LessonService;
import com.tokenlearn.server.service.NotificationService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.tokenlearn.server.controller.RestResponses.ok;

/**
 * Endpoints for the scheduled lesson lifecycle, including completion, cancellation, ratings, and lesson messaging.
 */
@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    private final LessonService lessonService;
    private final NotificationService notificationService;
    private final AdminService adminService;

    public LessonController(
            LessonService lessonService,
            NotificationService notificationService,
            AdminService adminService) {
        this.lessonService = lessonService;
        this.notificationService = notificationService;
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            Authentication authentication,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String participant,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer userId = AuthUtil.requireUserId(authentication);

        if ("all".equalsIgnoreCase(participant)) {
            adminService.requireAdmin(userId);
            Map<String, Object> payload = new LinkedHashMap<>(adminService.listLessons(status, limit, offset));
            Object lessons = payload.remove("lessons");
            payload.put("items", lessons == null ? List.of() : lessons);
            return ok(payload);
        }

        if (from != null && to != null) {
            Map<String, Object> calendar = new LinkedHashMap<>(lessonService.calendar(userId, role, status, from, to));
            Object lessons = calendar.remove("lessons");
            calendar.put("items", lessons == null ? List.of() : lessons);
            return ok(calendar);
        }

        if (from != null || to != null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_RANGE", "Both from and to must be provided together");
        }

        if (isHistoryStatusFilter(status)) {
            List<Map<String, Object>> lessons = lessonService.history(userId, limit, offset);
            return ok(Map.of(
                    "items", lessons,
                    "totalCount", lessonService.historyCount(userId)));
        }

        if (status == null || status.isBlank() || "scheduled".equalsIgnoreCase(status)) {
            return ok(Map.of("items", lessonService.upcoming(userId, role)));
        }

        throw new AppException(
                HttpStatus.BAD_REQUEST,
                "INVALID_QUERY",
                "Use participant=all, status=scheduled, status=completed,cancelled, or provide from/to");
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<Map<String, Object>> details(
            Authentication authentication,
            @PathVariable Integer lessonId) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.details(lessonId, userId));
    }

    @PatchMapping("/{lessonId}")
    public ResponseEntity<Map<String, Object>> updateState(
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

    @GetMapping("/{lessonId}/messages")
    public ResponseEntity<List<Map<String, Object>>> messages(
            Authentication authentication,
            @PathVariable Integer lessonId,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer userId = AuthUtil.requireUserId(authentication);
        lessonService.assertParticipant(lessonId, userId);
        return ok(notificationService.lessonMessageThreadForUser(userId, lessonId, limit, offset));
    }

    @PostMapping("/{lessonId}/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(
            Authentication authentication,
            @PathVariable Integer lessonId,
            @Valid @RequestBody CreateLessonMessageRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.sendLessonMessage(lessonId, userId, request));
    }

    private boolean isHistoryStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return "completed,cancelled".equals(normalized)
                || "cancelled,completed".equals(normalized);
    }
}
