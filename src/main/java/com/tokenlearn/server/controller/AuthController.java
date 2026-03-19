package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.*;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.service.AuthService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.tokenlearn.server.controller.ApiResponses.created;
import static com.tokenlearn.server.controller.ApiResponses.ok;

/**
 * Authentication endpoints for session lifecycle, registration, password reset resources, and token verification.
 */
@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/session")
    public ResponseEntity<ApiResponse<AuthPayloadDto>> login(@Valid @RequestBody AuthLoginRequest request) {
        return created(authService.login(request));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<AuthPayloadDto>> register(@Valid @RequestBody RegisterRequest request) {
        return created(authService.register(request));
    }

    @PostMapping("/password-reset-requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPasswordResetRequest(@Valid @RequestBody EmailRequest request) {
        return created(Map.of(
                "email", request.getEmail(),
                "secretQuestion", authService.getSecretQuestion(request.getEmail())));
    }

    @PostMapping("/password-reset-tokens")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPasswordResetToken(@Valid @RequestBody VerifySecretAnswerRequest request) {
        try {
            String resetToken = authService.verifySecretAnswerAndCreateResetToken(request);
            return created(Map.of(
                    "verified", true,
                    "email", request.getEmail(),
                    "resetToken", resetToken));
        } catch (AppException ex) {
            if ("INVALID_SECRET_ANSWER".equals(ex.getCode())) {
                return ok(Map.of("verified", false, "message", "Incorrect answer"));
            }
            throw ex;
        }
    }

    @PostMapping("/password-reset-completions")
    public ResponseEntity<ApiResponse<Map<String, String>>> completePasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return created(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/identity-providers/google/sessions")
    public ResponseEntity<ApiResponse<AuthPayloadDto>> createGoogleSession(@Valid @RequestBody GoogleAuthRequest request) {
        return created(authService.googleLogin(request));
    }

    @DeleteMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout() {
        return ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> currentSession(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(Map.of(
                "valid", true,
                "user", authService.verifyToken(userId)));
    }
}
