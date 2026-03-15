package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

/**
 * Admin payload for adjusting a user's available token balance with an audit reason.
 */
@Data
public class UpdateUserTokensRequest {
    @NotNull
    private BigDecimal amount;

    @Size(max = 200)
    @Pattern(regexp = NO_HTML_TAGS, message = "Reason must not contain HTML tags")
    private String reason;
}
