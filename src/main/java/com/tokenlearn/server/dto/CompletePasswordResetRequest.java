package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.RESET_TOKEN;

/**
 * Password-reset completion payload scoped to an existing password reset request resource.
 */
@Data
public class CompletePasswordResetRequest {
    @NotBlank
    @Pattern(regexp = RESET_TOKEN, message = "Invalid reset token format")
    private String resetToken;

    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;
}
