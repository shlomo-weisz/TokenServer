package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.*;
import com.tokenlearn.server.service.LessonRequestService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.tokenlearn.server.controller.ApiResponses.ok;

@RestController
@RequestMapping("/api/lesson-requests")
public class LessonRequestController {
    private final LessonRequestService lessonRequestService;

    public LessonRequestController(LessonRequestService lessonRequestService) {
        this.lessonRequestService = lessonRequestService;
    }

    @GetMapping("/student")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> asStudent(
            Authentication authentication,
            @RequestParam(required = false) String status) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonRequestService.listForStudent(userId, status));
    }

    @GetMapping("/teacher")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> asTeacher(
            Authentication authentication,
            @RequestParam(required = false) String status) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonRequestService.listForTutor(userId, status));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            Authentication authentication,
            @Valid @RequestBody CreateLessonRequestInputDto request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonRequestService.create(userId, request));
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(
            Authentication authentication,
            @PathVariable Integer requestId) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonRequestService.approve(requestId, userId));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(
            Authentication authentication,
            @PathVariable Integer requestId,
            @RequestBody RejectLessonRequestInputDto input) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonRequestService.reject(requestId, userId, input));
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancel(
            Authentication authentication,
            @PathVariable Integer requestId) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonRequestService.cancel(requestId, userId));
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelAlias(
            Authentication authentication,
            @PathVariable Integer requestId) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonRequestService.cancel(requestId, userId));
    }
}
