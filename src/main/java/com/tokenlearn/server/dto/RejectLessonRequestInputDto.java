package com.tokenlearn.server.dto;

import lombok.Data;

@Data
public class RejectLessonRequestInputDto {
    private String rejectionMessage;
    private String reason;
}
