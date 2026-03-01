package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String resetToken;

    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;
}
