package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.ApiResponse;
import com.tokenlearn.server.dto.AdminUpdateRatingRequest;
import com.tokenlearn.server.dto.AdminUpdateUserRequest;
import com.tokenlearn.server.dto.ContactAdminRequest;
import com.tokenlearn.server.dto.UpdateUserTokensRequest;
import com.tokenlearn.server.service.AdminService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.tokenlearn.server.controller.ApiResponses.ok;

/**
 * Administrative REST endpoints for moderation, reporting, and manual account maintenance.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        return ok(adminService.dashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> users(
            Authentication authentication,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String role) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        return ok(adminService.users(limit, offset, role));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> statistics(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        return ok(adminService.statistics());
    }

    @PostMapping("/contact")
    public ResponseEntity<ApiResponse<Map<String, Object>>> contact(
            Authentication authentication,
            @Valid @RequestBody ContactAdminRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(adminService.contact(userId, request.getSubject(), request.getMessage()));
    }

    @PostMapping("/tutors/{tutorId}/block")
    public ResponseEntity<ApiResponse<Map<String, Object>>> blockTutor(
            Authentication authentication,
            @PathVariable Integer tutorId) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        return ok(adminService.setTutorBlocked(tutorId, true));
    }

    @PostMapping("/tutors/{tutorId}/unblock")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unblockTutor(
            Authentication authentication,
            @PathVariable Integer tutorId) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        return ok(adminService.setTutorBlocked(tutorId, false));
    }

    @GetMapping("/lessons")
    public ResponseEntity<ApiResponse<Map<String, Object>>> lessons(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        return ok(adminService.listLessons(status, limit, offset));
    }

    @GetMapping("/ratings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ratings(
            Authentication authentication,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        return ok(adminService.listRatings(limit, offset));
    }

    @PutMapping("/users/{userId}/tokens")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTokens(
            Authentication authentication,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateUserTokensRequest request) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(adminId);
        return ok(adminService.adjustTokens(userId, request));
    }

    @GetMapping("/users/{userId}/tokens/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> userTokenHistory(
            Authentication authentication,
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        return ok(adminService.userTokenHistory(adminId, userId, limit, offset));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUser(
            Authentication authentication,
            @PathVariable Integer userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        return ok(adminService.updateUser(adminId, userId, request));
    }

    @PutMapping("/ratings/{ratingId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRating(
            Authentication authentication,
            @PathVariable Integer ratingId,
            @Valid @RequestBody AdminUpdateRatingRequest request) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        return ok(adminService.updateRating(adminId, ratingId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteUser(
            Authentication authentication,
            @PathVariable Integer userId) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        return ok(adminService.deleteUser(adminId, userId));
    }
}
