package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static com.tokenlearn.server.validation.InputValidationPatterns.TIME_24H;
import static com.tokenlearn.server.validation.InputValidationPatterns.TIME_OR_ISO_LOCAL_DATETIME;
import static com.tokenlearn.server.validation.InputValidationPatterns.WEEKDAY_EN_OR_HE;

/**
 * Structured description of the requested availability window and exact lesson time.
 */
@Data
public class RequestedSlotDto {
    @Pattern(regexp = WEEKDAY_EN_OR_HE, message = "Day must be one of Sunday..Saturday (English or Hebrew)")
    private String day;

    @Pattern(regexp = TIME_24H, message = "Start time must be in HH:mm format")
    private String startTime;

    @Pattern(regexp = TIME_24H, message = "End time must be in HH:mm format")
    private String endTime;

    @Pattern(regexp = TIME_OR_ISO_LOCAL_DATETIME, message = "Specific start time must be HH:mm or ISO local datetime")
    private String specificStartTime;

    @Pattern(regexp = TIME_OR_ISO_LOCAL_DATETIME, message = "Specific end time must be HH:mm or ISO local datetime")
    private String specificEndTime;
}
