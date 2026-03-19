package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.*;
import com.tokenlearn.server.service.UserService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static com.tokenlearn.server.controller.RestResponses.ok;

/**
 * Endpoints for the authenticated profile singleton and public user profile lookups.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> profile(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(userService.getProfile(userId, true));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileDto> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(userService.updateProfile(userId, request));
    }

    @PutMapping("/me/photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) {
        Integer userId = AuthUtil.requireUserId(authentication);
        String photoUrl = userService.updatePhoto(userId, file.getOriginalFilename());
        return ok(Map.of("photoUrl", photoUrl));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDto> byId(@PathVariable Integer userId) {
        return ok(userService.getProfile(userId, false));
    }

    @GetMapping("/{userId}/ratings")
    public ResponseEntity<Map<String, Object>> ratings(@PathVariable Integer userId) {
        return ok(userService.getUserRatings(userId));
    }
}
