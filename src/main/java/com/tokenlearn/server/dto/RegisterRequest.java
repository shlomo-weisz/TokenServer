package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.NAME;
import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;
import static com.tokenlearn.server.validation.InputValidationPatterns.PHONE;

/**
 * Registration payload for password-based account creation and recovery-question setup.
 */
@Data
public class RegisterRequest {
    @Email
    @NotBlank
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = NAME, message = "First name may contain letters, spaces, apostrophes, and hyphens only")
    private String firstName;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = NAME, message = "Last name may contain letters, spaces, apostrophes, and hyphens only")
    private String lastName;

    @Size(max = 20)
    @Pattern(regexp = PHONE, message = "Phone must contain digits and optional +, spaces, parentheses, or hyphens")
    private String phone;

    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = NO_HTML_TAGS, message = "Secret question must not contain HTML tags")
    private String secretQuestion;

    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = NO_HTML_TAGS, message = "Secret answer must not contain HTML tags")
    private String secretAnswer;
}
