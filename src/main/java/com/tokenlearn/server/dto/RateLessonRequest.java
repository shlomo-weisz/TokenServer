package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RateLessonRequest {
    @NotNull
    private BigDecimal rating;
    private String comment;
}
