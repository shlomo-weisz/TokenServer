package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.AdminUpdateRatingRequest;
import com.tokenlearn.server.dto.CreateRatingRequest;
import com.tokenlearn.server.dto.RateLessonRequest;
import com.tokenlearn.server.service.AdminService;
import com.tokenlearn.server.service.LessonService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.tokenlearn.server.controller.RestResponses.created;
import static com.tokenlearn.server.controller.RestResponses.ok;

/**
 * REST endpoints for lesson rating resources.
 */
@RestController
@RequestMapping("/api/ratings")
public class RatingController {
    private final LessonService lessonService;
    private final AdminService adminService;

    public RatingController(LessonService lessonService, AdminService adminService) {
        this.lessonService = lessonService;
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        Map<String, Object> payload = new LinkedHashMap<>(adminService.listRatings(limit, offset));
        Object ratings = payload.remove("ratings");
        payload.put("items", ratings);
        return ok(payload);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            Authentication authentication,
            @Valid @RequestBody CreateRatingRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        Map<String, Object> payload = lessonService.createRating(userId, request);
        Object ratingId = payload.get("ratingId");
        URI location = ratingId == null ? URI.create("/api/ratings") : URI.create("/api/ratings/" + ratingId);
        return created(location, payload);
    }

    @PatchMapping("/{ratingId}")
    public ResponseEntity<Map<String, Object>> update(
            Authentication authentication,
            @PathVariable Integer ratingId,
            @Valid @RequestBody RateLessonRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        if (adminService.isAdmin(userId)) {
            AdminUpdateRatingRequest adminRequest = new AdminUpdateRatingRequest();
            adminRequest.setRating(request.getRating());
            adminRequest.setComment(request.getComment());
            return ok(adminService.updateRating(userId, ratingId, adminRequest));
        }
        return ok(lessonService.updateRating(ratingId, userId, request));
    }
}
