package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Payload for tutor rejection of a lesson request, including optional reason aliases.
 */
@Data
public class RejectLessonRequestInputDto {
    @Size(max = 500)
    @Pattern(regexp = NO_HTML_TAGS, message = "Rejection message must not contain HTML tags")
    private String rejectionMessage;

    @Size(max = 500)
    @Pattern(regexp = NO_HTML_TAGS, message = "Reason must not contain HTML tags")
    private String reason;
}
