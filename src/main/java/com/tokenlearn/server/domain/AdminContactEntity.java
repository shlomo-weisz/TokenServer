package com.tokenlearn.server.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Contact thread opened by a user for the shared admin inbox workflow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminContactEntity {
    private Long contactId;
    private Integer userId;
    private String subject;
    private String message;
    private String status;
    private LocalDateTime submittedAt;
}
