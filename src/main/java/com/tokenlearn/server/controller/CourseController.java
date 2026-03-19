package com.tokenlearn.server.controller;

import com.tokenlearn.server.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.tokenlearn.server.controller.RestResponses.ok;

/**
 * Read-only course catalog endpoints used by course search and category selection flows.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> all(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer limit) {
        return ok(courseService.getCourses(search, category, limit));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ok(courseService.getCategories());
    }
}
