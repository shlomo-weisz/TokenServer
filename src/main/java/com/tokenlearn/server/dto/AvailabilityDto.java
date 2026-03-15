package com.tokenlearn.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Availability slot returned to clients after weekday and time normalization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityDto {
    private Integer id;
    private String day;
    private String startTime;
    private String endTime;
    private Boolean isAvailable;
}
