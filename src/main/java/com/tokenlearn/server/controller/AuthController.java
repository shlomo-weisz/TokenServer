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

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthPayloadDto>> login(@Valid @RequestBody AuthLoginRequest request) {
        return ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthPayloadDto>> register(@Valid @RequestBody RegisterRequest request) {
        return created(authService.register(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthPayloadDto>> signupAlias(@Valid @RequestBody RegisterRequest request) {
        return created(authService.register(request));
    }

    @PostMapping("/secret-question")
    public ResponseEntity<ApiResponse<Map<String, Object>>> secretQuestion(@Valid @RequestBody EmailRequest request) {
        return ok(Map.of("secretQuestion", authService.getSecretQuestion(request.getEmail())));
    }

    @PostMapping("/verify-secret-answer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifySecretAnswer(@Valid @RequestBody VerifySecretAnswerRequest request) {
        try {
            String resetToken = authService.verifySecretAnswerAndCreateResetToken(request);
            return ok(Map.of("verified", true, "resetToken", resetToken));
        } catch (AppException ex) {
            if ("INVALID_SECRET_ANSWER".equals(ex.getCode())) {
                return ok(Map.of("verified", false, "message", "Incorrect answer"));
            }
            throw ex;
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthPayloadDto>> google(@Valid @RequestBody GoogleAuthRequest request) {
        return ok(authService.googleLogin(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout() {
        return ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verify(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(Map.of(
                "valid", true,
                "user", authService.verifyToken(userId)));
    }
}
