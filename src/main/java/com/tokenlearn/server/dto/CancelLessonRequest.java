package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Optional cancellation payload carrying a free-text reason for the other participant.
 */
@Data
public class CancelLessonRequest {
    @Size(max = 500)
    @Pattern(regexp = NO_HTML_TAGS, message = "Reason must not contain HTML tags")
    private String reason;
}
