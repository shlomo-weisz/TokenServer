package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifySecretAnswerRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String secretAnswer;
}
