package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateLessonRequestInputDto {
    @NotNull
    private Integer tutorId;
    private String tutorName;
    private Integer courseId;
    private String course;

    @NotNull
    @Positive
    private BigDecimal tokenCost;

    private RequestedSlotDto requestedSlot;
    private String message;
}
