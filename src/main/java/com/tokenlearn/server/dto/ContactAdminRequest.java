package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Message payload submitted by users when contacting administrators.
 */
@Data
public class ContactAdminRequest {
    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = NO_HTML_TAGS, message = "Subject must not contain HTML tags")
    private String subject;

    @NotBlank
    @Size(max = 2000)
    @Pattern(regexp = NO_HTML_TAGS, message = "Message must not contain HTML tags")
    private String message;
}
