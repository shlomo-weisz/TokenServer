package com.tokenlearn.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthPayloadDto {
    private String token;
    private UserSummaryDto user;
    private Boolean isNewUser;
    private Boolean isFirstFiftyUser;
    private Integer bonusTokens;
}
