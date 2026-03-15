package com.tokenlearn.server.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Persisted lesson request, including the requested slot, approval status, and any rejection details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonRequestEntity {
    private Integer requestId;
    private Integer studentId;
    private Integer tutorId;
    private Integer courseId;
    private BigDecimal tokenCost;
    private String requestedDay;
    private LocalTime requestedStartTime;
    private LocalTime requestedEndTime;
    private LocalDateTime specificStartTime;
    private LocalDateTime specificEndTime;
    private String message;
    private String status;
    private String rejectionMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
