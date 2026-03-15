package com.tokenlearn.server.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Payload for creating or updating a lesson rating.
 */
@Data
public class RateLessonRequest {
    @NotNull
    @DecimalMin(value = "1.0", message = "Rating must be at least 1")
    @DecimalMax(value = "5.0", message = "Rating must be at most 5")
    private BigDecimal rating;

    @Size(max = 1000)
    @Pattern(regexp = NO_HTML_TAGS, message = "Comment must not contain HTML tags")
    private String comment;
}
