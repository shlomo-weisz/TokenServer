package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferTokensRequest {
    @NotNull
    private Integer toUserId;
    @NotNull
    @Positive
    private BigDecimal amount;
    private Integer lessonId;
    private String reason;
}
