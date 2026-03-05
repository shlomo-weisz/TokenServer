package com.tokenlearn.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

import static com.tokenlearn.server.validation.InputValidationPatterns.NAME;
import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;
import static com.tokenlearn.server.validation.InputValidationPatterns.PHONE;
import static com.tokenlearn.server.validation.InputValidationPatterns.URL_HTTP_OR_BLOB;

@Data
public class UpdateUserProfileRequest {
    @Size(max = 50)
    @Pattern(regexp = NAME, message = "First name may contain letters, spaces, apostrophes, and hyphens only")
    private String firstName;

    @Size(max = 50)
    @Pattern(regexp = NAME, message = "Last name may contain letters, spaces, apostrophes, and hyphens only")
    private String lastName;

    @Size(max = 20)
    @Pattern(regexp = PHONE, message = "Phone must contain digits and optional +, spaces, parentheses, or hyphens")
    private String phone;

    @Size(max = 500)
    @Pattern(regexp = URL_HTTP_OR_BLOB, message = "Photo URL must be a valid http(s) or blob URL")
    private String photoUrl;

    @Valid
    private List<CourseSelectionDto> coursesAsTeacher;

    @Valid
    private List<CourseSelectionDto> coursesAsStudent;

    @Valid
    private List<AvailabilityInputDto> availabilityAsTeacher;

    @Valid
    private List<AvailabilityInputDto> availabilityAsStudent;

    @Size(max = 2000)
    @Pattern(regexp = NO_HTML_TAGS, message = "About me as teacher must not contain HTML tags")
    private String aboutMeAsTeacher;

    @Size(max = 2000)
    @Pattern(regexp = NO_HTML_TAGS, message = "About me as student must not contain HTML tags")
    private String aboutMeAsStudent;
}
