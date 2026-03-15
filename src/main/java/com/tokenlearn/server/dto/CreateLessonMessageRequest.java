package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Request body for sending a chat-style message within a scheduled lesson.
 */
@Data
public class CreateLessonMessageRequest {
    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must be at most 2000 characters")
    @Pattern(regexp = NO_HTML_TAGS, message = "Message must not contain HTML tags")
    private String message;
}
