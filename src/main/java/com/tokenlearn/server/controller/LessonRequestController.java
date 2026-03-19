package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.*;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.service.LessonRequestService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.tokenlearn.server.controller.RestResponses.created;
import static com.tokenlearn.server.controller.RestResponses.ok;

/**
 * Endpoints for creating, reviewing, and cancelling lesson requests before they become scheduled lessons.
 */
@RestController
@RequestMapping("/api/lesson-requests")
public class LessonRequestController {
    private final LessonRequestService lessonRequestService;

    public LessonRequestController(LessonRequestService lessonRequestService) {
        this.lessonRequestService = lessonRequestService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            Authentication authentication,
            @RequestParam String role,
            @RequestParam(required = false) String status) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return switch (role.trim().toLowerCase()) {
            case "student" -> ok(lessonRequestService.listForStudent(userId, status));
            case "teacher" -> ok(lessonRequestService.listForTutor(userId, status));
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "Role must be student or teacher");
        };
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            Authentication authentication,
            @Valid @RequestBody CreateLessonRequestInputDto request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        Map<String, Object> payload = lessonRequestService.create(userId, request);
        Object requestId = payload.get("requestId");
        URI location = requestId == null
                ? URI.create("/api/lesson-requests")
                : URI.create("/api/lesson-requests/" + requestId);
        return created(location, payload);
    }

    @PatchMapping("/{requestId}")
    public ResponseEntity<Map<String, Object>> updateStatus(
            Authentication authentication,
            @PathVariable Integer requestId,
            @Valid @RequestBody UpdateLessonRequestStatusRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return switch (request.getStatus().trim().toUpperCase()) {
            case "APPROVED" -> ok(lessonRequestService.approve(requestId, userId));
            case "REJECTED" -> {
                RejectLessonRequestInputDto rejectRequest = new RejectLessonRequestInputDto();
                rejectRequest.setReason(request.getReason());
                rejectRequest.setRejectionMessage(request.getReason());
                yield ok(lessonRequestService.reject(requestId, userId, rejectRequest));
            }
            case "CANCELLED" -> ok(lessonRequestService.cancel(requestId, userId));
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Unsupported lesson request status");
        };
    }
}
