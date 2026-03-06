package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.ApiResponse;
import com.tokenlearn.server.dto.AvailabilityDto;
import com.tokenlearn.server.service.TutorService;
import com.tokenlearn.server.util.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.tokenlearn.server.controller.ApiResponses.ok;

@RestController
@RequestMapping("/api/tutors")
public class TutorController {
    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> recommended(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") BigDecimal minRating) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(tutorService.recommended(userId, limit, minRating));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> search(
            Authentication authentication,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") BigDecimal minRating,
            @RequestParam(defaultValue = "20") int limit) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(tutorService.search(userId, course, name, minRating, limit));
    }

    @GetMapping("/{tutorId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> profile(@PathVariable Integer tutorId) {
        return ok(tutorService.profile(tutorId));
    }

    @GetMapping("/{tutorId}/availability")
    public ResponseEntity<ApiResponse<List<AvailabilityDto>>> availability(@PathVariable Integer tutorId) {
        return ok(tutorService.availability(tutorId));
    }
}
