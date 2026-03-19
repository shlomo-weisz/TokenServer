package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.*;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.service.AuthService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.tokenlearn.server.controller.RestResponses.created;
import static com.tokenlearn.server.controller.RestResponses.noContent;
import static com.tokenlearn.server.controller.RestResponses.ok;

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

    @PostMapping("/sessions")
    public ResponseEntity<AuthPayloadDto> createSession(@Valid @RequestBody CreateSessionRequest request) {
        if (StringUtils.hasText(request.getGoogleToken())) {
            GoogleAuthRequest googleAuthRequest = new GoogleAuthRequest();
            googleAuthRequest.setGoogleToken(request.getGoogleToken());
            return created(URI.create("/api/sessions/current"), authService.googleLogin(googleAuthRequest));
        }
        if (!StringUtils.hasText(request.getEmail()) || !StringUtils.hasText(request.getPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_SESSION_REQUEST", "Email/password or googleToken is required");
        }
        AuthLoginRequest loginRequest = new AuthLoginRequest();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setPassword(request.getPassword());
        return created(URI.create("/api/sessions/current"), authService.login(loginRequest));
    }

    @PostMapping("/users")
    public ResponseEntity<AuthPayloadDto> register(@Valid @RequestBody RegisterRequest request) {
        AuthPayloadDto payload = authService.register(request);
        Integer userId = payload.getUser() == null ? null : payload.getUser().getId();
        URI location = userId == null ? URI.create("/api/users") : URI.create("/api/users/" + userId);
        return created(location, payload);
    }

    @PostMapping("/password-reset-requests")
    public ResponseEntity<Map<String, Object>> createPasswordResetRequest(@Valid @RequestBody EmailRequest request) {
        String encodedEmail = UriUtils.encodePathSegment(request.getEmail(), StandardCharsets.UTF_8);
        return created(URI.create("/api/password-reset-requests/" + encodedEmail), Map.of(
                "id", request.getEmail(),
                "email", request.getEmail(),
                "secretQuestion", authService.getSecretQuestion(request.getEmail()),
                "status", "pending_secret_answer"));
    }

    @PatchMapping("/password-reset-requests/{email}")
    public ResponseEntity<Map<String, Object>> verifyPasswordResetRequest(
            @PathVariable String email,
            @Valid @RequestBody VerifyPasswordResetAnswerRequest request) {
        VerifySecretAnswerRequest verification = new VerifySecretAnswerRequest();
        verification.setEmail(email);
        verification.setSecretAnswer(request.getSecretAnswer());
        String resetToken = authService.verifySecretAnswerAndCreateResetToken(verification);
        return ok(Map.of(
                "id", email,
                "email", email,
                "status", "verified",
                "resetToken", resetToken));
    }

    @PutMapping("/password-reset-requests/{email}/password")
    public ResponseEntity<Void> completePasswordReset(
            @PathVariable String email,
            @Valid @RequestBody CompletePasswordResetRequest request) {
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        resetPasswordRequest.setEmail(email);
        resetPasswordRequest.setResetToken(request.getResetToken());
        resetPasswordRequest.setNewPassword(request.getNewPassword());
        authService.resetPassword(resetPasswordRequest);
        return noContent();
    }

    @DeleteMapping("/sessions/current")
    public ResponseEntity<Void> deleteCurrentSession() {
        return noContent();
    }

    @GetMapping("/sessions/current")
    public ResponseEntity<Map<String, Object>> currentSession(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(Map.of(
                "id", "current",
                "authenticated", true,
                "user", authService.verifyToken(userId)));
    }
}
