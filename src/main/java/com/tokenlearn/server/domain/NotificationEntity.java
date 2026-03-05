package com.tokenlearn.server.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {
    private Long notificationId;
    private Integer userId;
    private String eventType;
    private Integer requestId;
    private Integer lessonId;
    private String counterpartName;
    private String courseName;
    private LocalDateTime scheduledAt;
    private String rejectionReason;
    private String actionPath;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
