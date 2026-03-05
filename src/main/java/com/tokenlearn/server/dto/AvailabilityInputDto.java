package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.TIME_24H;
import static com.tokenlearn.server.validation.InputValidationPatterns.WEEKDAY_EN_OR_HE;

@Data
public class AvailabilityInputDto {
    @Positive
    private Integer id;

    @Pattern(regexp = WEEKDAY_EN_OR_HE, message = "Day must be one of Sunday..Saturday (English or Hebrew)")
    private String day;

    @Pattern(regexp = TIME_24H, message = "Start time must be in HH:mm format")
    private String startTime;

    @Pattern(regexp = TIME_24H, message = "End time must be in HH:mm format")
    private String endTime;
}
