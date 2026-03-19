package com.tokenlearn.server.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Unified token transaction payload for purchases, peer transfers, and admin adjustments.
 */
@Data
public class CreateTokenTransactionRequest {
    @Size(max = 50)
    @Pattern(regexp = NO_HTML_TAGS, message = "Type must not contain HTML tags")
    private String type;

    private BigDecimal amount;

    @Size(max = 50)
    @Pattern(regexp = NO_HTML_TAGS, message = "Payment method must not contain HTML tags")
    private String paymentMethod;

    private Map<String, Object> paymentDetails;

    private Integer toUserId;

    private Integer lessonId;

    @Size(max = 200)
    @Pattern(regexp = NO_HTML_TAGS, message = "Reason must not contain HTML tags")
    private String reason;
}
