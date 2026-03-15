package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.GOOGLE_ID_TOKEN;

/**
 * Request carrying a Google ID token for sign-in.
 */
@Data
public class GoogleAuthRequest {
    @NotBlank
    @Size(max = 5000)
    @Pattern(regexp = GOOGLE_ID_TOKEN, message = "Invalid Google token format")
    private String googleToken;
}
