package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Partial update payload for changing a lesson resource state.
 */
@Data
public class UpdateLessonStateRequest {
    @NotBlank
    @Pattern(regexp = "COMPLETED|CANCELLED", message = "Status must be COMPLETED or CANCELLED")
    private String status;

    @Size(max = 500)
    @Pattern(regexp = NO_HTML_TAGS, message = "Reason must not contain HTML tags")
    private String reason;
}
