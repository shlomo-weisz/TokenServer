package com.tokenlearn.server.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ledger entry that records a token balance movement for audit, reporting, and history views.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenTransactionEntity {
    private Long txId;
    private Integer requestId;
    private Integer lessonId;
    private Integer payerId;
    private Integer receiverId;
    private BigDecimal amount;
    private String txType;
    private String status;
    private String messageId;
    private String description;
    private LocalDateTime createdAt;
}
