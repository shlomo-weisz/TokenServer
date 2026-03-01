package com.tokenlearn.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDto {
    private Integer id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String photoUrl;
    private Boolean isAdmin;
}
