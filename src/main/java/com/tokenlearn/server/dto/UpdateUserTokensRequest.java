package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateUserTokensRequest {
    @NotNull
    private BigDecimal amount;
    private String reason;
}
