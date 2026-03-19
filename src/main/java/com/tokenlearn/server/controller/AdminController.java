package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.ApiResponse;
import com.tokenlearn.server.dto.AdminUpdateRatingRequest;
import com.tokenlearn.server.dto.AdminUpdateUserRequest;
import com.tokenlearn.server.dto.ContactAdminRequest;
import com.tokenlearn.server.dto.CreateAdminContactReplyRequest;
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

    @GetMapping("/analytics/summary")
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

    @GetMapping("/analytics/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> statistics(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        return ok(adminService.statistics());
    }

    @PostMapping("/contacts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> contact(
            Authentication authentication,
            @Valid @RequestBody ContactAdminRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(adminService.contact(userId, request.getSubject(), request.getMessage()));
    }

    @GetMapping("/contacts/{contactId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> contactThread(
            Authentication authentication,
            @PathVariable Long contactId) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(adminService.contactThread(userId, contactId));
    }

    @PostMapping("/contacts/{contactId}/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> replyToContact(
            Authentication authentication,
            @PathVariable Long contactId,
            @Valid @RequestBody CreateAdminContactReplyRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(adminService.replyToContact(userId, contactId, request.getMessage()));
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

    @PostMapping("/users/{userId}/token-adjustments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTokens(
            Authentication authentication,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateUserTokensRequest request) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(adminId);
        return ok(adminService.adjustTokens(userId, request));
    }

    @GetMapping("/users/{userId}/token-transactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> userTokenHistory(
            Authentication authentication,
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        return ok(adminService.userTokenHistory(adminId, userId, limit, offset));
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUser(
            Authentication authentication,
            @PathVariable Integer userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        return ok(adminService.updateUser(adminId, userId, request));
    }

    @PatchMapping("/ratings/{ratingId}")
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
