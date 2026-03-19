package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.GOOGLE_ID_TOKEN;

/**
 * Session creation payload supporting either email/password authentication or a Google ID token.
 */
@Data
public class CreateSessionRequest {
    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 100)
    private String password;

    @Size(max = 5000)
    @Pattern(regexp = GOOGLE_ID_TOKEN, message = "Invalid Google token format")
    private String googleToken;
}
