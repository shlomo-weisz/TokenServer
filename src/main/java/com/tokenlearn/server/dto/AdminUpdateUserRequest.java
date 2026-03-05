package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.NAME;
import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;
import static com.tokenlearn.server.validation.InputValidationPatterns.PHONE;
import static com.tokenlearn.server.validation.InputValidationPatterns.URL_HTTP_OR_BLOB;

@Data
public class AdminUpdateUserRequest {
    @Email
    @NotBlank
    @Size(max = 254)
    private String email;

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

    @Size(max = 500)
    @Pattern(regexp = URL_HTTP_OR_BLOB, message = "Photo URL must be a valid http(s) or blob URL")
    private String photoUrl;

    @Size(max = 2000)
    @Pattern(regexp = NO_HTML_TAGS, message = "About me as teacher must not contain HTML tags")
    private String aboutMeAsTeacher;

    @Size(max = 2000)
    @Pattern(regexp = NO_HTML_TAGS, message = "About me as student must not contain HTML tags")
    private String aboutMeAsStudent;

    @NotNull
    private Boolean isAdmin;

    @NotNull
    private Boolean isBlockedTutor;

    @NotNull
    private Boolean isActive;
}
