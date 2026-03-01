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
public class RatingEntity {
    private Integer ratingId;
    private Integer lessonId;
    private Integer fromUserId;
    private Integer toUserId;
    private BigDecimal score;
    private String comment;
    private LocalDateTime createdAt;
}
