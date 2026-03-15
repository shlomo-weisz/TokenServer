package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.RESET_TOKEN;

/**
 * Password reset payload containing email, reset token, and the new password.
 */
@Data
public class ResetPasswordRequest {
    @Email
    @NotBlank
    @Size(max = 254)
    private String email;

    @NotBlank
    @Pattern(regexp = RESET_TOKEN, message = "Invalid reset token format")
    private String resetToken;

    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;
}
