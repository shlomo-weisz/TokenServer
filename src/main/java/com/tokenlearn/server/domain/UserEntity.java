package com.tokenlearn.server.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    private Integer userId;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String phone;
    private String photoUrl;
    private String secretQuestion;
    private String secretAnswerHash;
    private String aboutMeAsTeacher;
    private String aboutMeAsStudent;
    private Boolean isAdmin;
    private Boolean isActive;
    private Boolean isBlockedTutor;
    private BigDecimal availableBalance;
    private BigDecimal lockedBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
