package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Payload for creating a new lesson rating resource.
 */
@Data
public class CreateRatingRequest extends RateLessonRequest {
    @NotNull
    @Positive
    private Integer lessonId;
}
