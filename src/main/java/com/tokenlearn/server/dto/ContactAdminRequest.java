package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactAdminRequest {
    @NotBlank
    private String subject;
    @NotBlank
    private String message;
}
