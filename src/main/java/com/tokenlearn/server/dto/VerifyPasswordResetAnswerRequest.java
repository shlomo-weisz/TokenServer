package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Secret-answer payload used to progress a password reset request resource.
 */
@Data
public class VerifyPasswordResetAnswerRequest {
    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = NO_HTML_TAGS, message = "Secret answer must not contain HTML tags")
    private String secretAnswer;
}
