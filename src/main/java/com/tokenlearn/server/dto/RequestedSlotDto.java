package com.tokenlearn.server.dto;

import lombok.Data;

@Data
public class RequestedSlotDto {
    private String day;
    private String startTime;
    private String endTime;
    private String specificStartTime;
    private String specificEndTime;
}
