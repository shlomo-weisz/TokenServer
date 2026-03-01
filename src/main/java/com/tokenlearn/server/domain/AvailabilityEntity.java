package com.tokenlearn.server.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityEntity {
    private Integer availabilityId;
    private Integer userId;
    private String role;
    private String day;
    private LocalTime startTime;
    private LocalTime endTime;
}
