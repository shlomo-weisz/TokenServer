package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.AdminUpdateUserRequest;
import com.tokenlearn.server.dto.ContactAdminRequest;
import com.tokenlearn.server.dto.CreateTokenTransactionRequest;
import com.tokenlearn.server.dto.CreateAdminContactReplyRequest;
import com.tokenlearn.server.dto.UpdateUserTokensRequest;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.service.AdminService;
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
 * Administrative REST endpoints for moderation, reporting, and manual account maintenance.
 */
@RestController
@RequestMapping("/api")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin/reports/summary")
    public ResponseEntity<Map<String, Object>> dashboard(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        return ok(adminService.dashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> users(
            Authentication authentication,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String role) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        return ok(adminService.users(limit, offset, role));
    }

    @GetMapping("/admin/reports/statistics")
    public ResponseEntity<Map<String, Object>> statistics(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(userId);
        return ok(adminService.statistics());
    }

    @PostMapping("/support-threads")
    public ResponseEntity<Map<String, Object>> contact(
            Authentication authentication,
            @Valid @RequestBody ContactAdminRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        Map<String, Object> payload = adminService.contact(userId, request.getSubject(), request.getMessage());
        Object contactId = payload.get("id");
        URI location = contactId == null
                ? URI.create("/api/support-threads")
                : URI.create("/api/support-threads/" + contactId);
        return created(location, payload);
    }

    @GetMapping("/support-threads/{contactId}")
    public ResponseEntity<Map<String, Object>> contactThread(
            Authentication authentication,
            @PathVariable Long contactId) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(adminService.contactThread(userId, contactId));
    }

    @PostMapping("/support-threads/{contactId}/messages")
    public ResponseEntity<Map<String, Object>> replyToContact(
            Authentication authentication,
            @PathVariable Long contactId,
            @Valid @RequestBody CreateAdminContactReplyRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(adminService.replyToContact(userId, contactId, request.getMessage()));
    }

    @PostMapping("/users/{userId}/token-transactions")
    public ResponseEntity<Map<String, Object>> updateTokens(
            Authentication authentication,
            @PathVariable Integer userId,
            @Valid @RequestBody CreateTokenTransactionRequest request) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        adminService.requireAdmin(adminId);
        if (request.getAmount() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "amount is required");
        }
        UpdateUserTokensRequest adjustment = new UpdateUserTokensRequest();
        adjustment.setAmount(request.getAmount());
        adjustment.setReason(request.getReason());
        return ok(adminService.adjustTokens(userId, adjustment));
    }

    @GetMapping("/users/{userId}/token-transactions")
    public ResponseEntity<Map<String, Object>> userTokenHistory(
            Authentication authentication,
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        return ok(adminService.userTokenHistory(adminId, userId, limit, offset));
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            Authentication authentication,
            @PathVariable Integer userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        return ok(adminService.updateUser(adminId, userId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            Authentication authentication,
            @PathVariable Integer userId) {
        Integer adminId = AuthUtil.requireUserId(authentication);
        return ok(adminService.deleteUser(adminId, userId));
    }
}
