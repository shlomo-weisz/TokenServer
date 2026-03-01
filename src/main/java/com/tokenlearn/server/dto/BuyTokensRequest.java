package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class BuyTokensRequest {
    @NotNull
    @Positive
    private BigDecimal amount;
    private String paymentMethod;
    private Map<String, Object> paymentDetails;
}
