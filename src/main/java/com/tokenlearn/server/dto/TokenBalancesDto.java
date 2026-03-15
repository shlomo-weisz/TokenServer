package com.tokenlearn.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Token wallet breakdown separating total, available, locked, and forecasted balances.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBalancesDto {
    private BigDecimal total;
    private BigDecimal available;
    private BigDecimal locked;
    private BigDecimal futureTutorEarnings;
    private BigDecimal pendingTransfers;
}
