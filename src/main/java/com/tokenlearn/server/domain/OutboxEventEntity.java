package com.tokenlearn.server.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persisted transactional outbox event that will be published asynchronously to the message broker.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEventEntity {
    private Long outboxId;
    private String aggregateType;
    private Integer aggregateId;
    private String eventType;
    private String payloadJson;
    private String messageId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String errorMessage;
}
