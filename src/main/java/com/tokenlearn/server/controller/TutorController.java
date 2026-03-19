package com.tokenlearn.server.controller;

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

import static com.tokenlearn.server.controller.RestResponses.ok;

/**
 * Tutor discovery endpoints covering filtered listings, public profiles, and availability.
 */
@RestController
@RequestMapping("/api")
public class TutorController {
    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping("/tutors")
    public ResponseEntity<List<Map<String, Object>>> search(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean recommended,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean taughtMeBefore,
            @RequestParam(defaultValue = "0") BigDecimal minRating,
            @RequestParam(defaultValue = "20") int limit) {
        Integer userId = AuthUtil.requireUserId(authentication);
        if (recommended) {
            return ok(tutorService.recommended(userId, limit, minRating));
        }
        return ok(tutorService.search(userId, course, name, minRating, taughtMeBefore, limit));
    }

    @GetMapping("/tutors/{tutorId}")
    public ResponseEntity<Map<String, Object>> profile(@PathVariable Integer tutorId) {
        return ok(tutorService.profile(tutorId));
    }

    @GetMapping("/tutors/{tutorId}/availability")
    public ResponseEntity<List<AvailabilityDto>> availability(@PathVariable Integer tutorId) {
        return ok(tutorService.availability(tutorId));
    }
}
