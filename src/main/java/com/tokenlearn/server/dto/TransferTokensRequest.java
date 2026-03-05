package com.tokenlearn.server.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

import static com.tokenlearn.server.validation.InputValidationPatterns.NO_HTML_TAGS;

@Data
public class TransferTokensRequest {
    @NotNull
    @Positive
    private Integer toUserId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @Positive
    private Integer lessonId;

    @Size(max = 200)
    @Pattern(regexp = NO_HTML_TAGS, message = "Reason must not contain HTML tags")
    private String reason;
}
