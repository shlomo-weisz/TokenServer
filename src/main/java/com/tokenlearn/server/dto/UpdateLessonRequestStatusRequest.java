package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Partial update payload for changing the status of a lesson request resource.
 */
@Data
public class UpdateLessonRequestStatusRequest {
    @NotBlank
    @Pattern(regexp = "APPROVED|REJECTED|CANCELLED", message = "Status must be APPROVED, REJECTED, or CANCELLED")
    private String status;

    @Size(max = 500)
    @Pattern(regexp = NO_HTML_TAGS, message = "Reason must not contain HTML tags")
    private String reason;
}
