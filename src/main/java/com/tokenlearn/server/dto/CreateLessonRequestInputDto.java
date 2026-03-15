package com.tokenlearn.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Payload for creating a lesson request with tutor, course, requested slot, and token cost.
 */
@Data
public class CreateLessonRequestInputDto {
    @NotNull
    @Positive
    private Integer tutorId;

    @Size(max = 120)
    @Pattern(regexp = NO_HTML_TAGS, message = "Tutor name must not contain HTML tags")
    private String tutorName;

    @Positive
    private Integer courseId;

    @Size(max = 255)
    @Pattern(regexp = NO_HTML_TAGS, message = "Course must not contain HTML tags")
    private String course;

    @NotNull
    @Positive
    private BigDecimal tokenCost;

    @NotNull
    @Valid
    private RequestedSlotDto requestedSlot;

    @Size(max = 2000)
    @Pattern(regexp = NO_HTML_TAGS, message = "Message must not contain HTML tags")
    private String message;
}
