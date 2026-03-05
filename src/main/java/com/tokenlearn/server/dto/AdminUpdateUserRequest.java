package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String phone;
    private String photoUrl;
    private String aboutMeAsTeacher;
    private String aboutMeAsStudent;

    @NotNull
    private Boolean isAdmin;

    @NotNull
    private Boolean isBlockedTutor;

    @NotNull
    private Boolean isActive;
}
