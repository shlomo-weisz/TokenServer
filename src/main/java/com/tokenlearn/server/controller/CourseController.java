package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.ApiResponse;
import com.tokenlearn.server.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.tokenlearn.server.controller.ApiResponses.ok;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> all(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        return ok(Map.of("courses", courseService.getCourses(search, category)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<Map<String, Object>>> categories() {
        return ok(Map.of("categories", courseService.getCategories()));
    }
}
