package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.ApiResponse;
import com.tokenlearn.server.dto.CreateRatingRequest;
import com.tokenlearn.server.dto.RateLessonRequest;
import com.tokenlearn.server.service.LessonService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.tokenlearn.server.controller.ApiResponses.ok;

/**
 * REST endpoints for lesson rating resources.
 */
@RestController
@RequestMapping("/api/ratings")
public class RatingController {
    private final LessonService lessonService;

    public RatingController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            Authentication authentication,
            @Valid @RequestBody CreateRatingRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.createRating(userId, request));
    }

    @PatchMapping("/{ratingId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            Authentication authentication,
            @PathVariable Integer ratingId,
            @Valid @RequestBody RateLessonRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(lessonService.updateRating(ratingId, userId, request));
    }
}
