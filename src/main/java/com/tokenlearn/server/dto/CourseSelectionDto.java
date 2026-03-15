package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Flexible course reference used in profile updates and course-selection UIs.
 */
@Data
public class CourseSelectionDto {
    @Positive
    private Integer id;

    @Size(max = 20)
    @Pattern(regexp = NO_HTML_TAGS, message = "Course number must not contain HTML tags")
    private String courseNumber;

    @Size(max = 255)
    @Pattern(regexp = NO_HTML_TAGS, message = "Course name must not contain HTML tags")
    private String nameHe;

    @Size(max = 255)
    @Pattern(regexp = NO_HTML_TAGS, message = "Course name must not contain HTML tags")
    private String nameEn;

    @Size(max = 255)
    @Pattern(regexp = NO_HTML_TAGS, message = "Course name must not contain HTML tags")
    private String name;
}
