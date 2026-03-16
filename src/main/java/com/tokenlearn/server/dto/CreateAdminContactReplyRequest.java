package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Request body for replying inside a private contact thread shared with administrators.
 */
@Data
public class CreateAdminContactReplyRequest {
    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must be at most 2000 characters")
    @Pattern(regexp = NO_HTML_TAGS, message = "Message must not contain HTML tags")
    private String message;
}
