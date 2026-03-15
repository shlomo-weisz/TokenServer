package com.tokenlearn.server.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persisted scheduled lesson created after a lesson request has been approved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonEntity {
    private Integer lessonId;
    private Integer requestId;
    private Integer studentId;
    private Integer tutorId;
    private Integer courseId;
    private BigDecimal tokenCost;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
