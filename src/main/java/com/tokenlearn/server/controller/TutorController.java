package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.ApiResponse;
import com.tokenlearn.server.dto.AvailabilityDto;
import com.tokenlearn.server.service.TutorService;
import org.springframework.http.ResponseEntity;
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
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") BigDecimal minRating) {
        return ok(tutorService.recommended(limit, minRating));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> search(
            @RequestParam(required = false) String course,
            @RequestParam(defaultValue = "0") BigDecimal minRating,
            @RequestParam(defaultValue = "20") int limit) {
        return ok(tutorService.search(course, minRating, limit));
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
