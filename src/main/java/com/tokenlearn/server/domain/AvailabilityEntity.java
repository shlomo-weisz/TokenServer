package com.tokenlearn.server.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Persistence model for recurring availability slots attached to a user acting as a teacher or student.
 */
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
