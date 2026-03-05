package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

@Data
public class BuyTokensRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    @Size(max = 50)
    @Pattern(regexp = NO_HTML_TAGS, message = "Payment method must not contain HTML tags")
    private String paymentMethod;

    private Map<String, Object> paymentDetails;
}
