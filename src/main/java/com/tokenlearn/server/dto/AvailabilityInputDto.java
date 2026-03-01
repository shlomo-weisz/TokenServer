package com.tokenlearn.server.dto;

import lombok.Data;

@Data
public class AvailabilityInputDto {
    private Integer id;
    private String day;
    private String startTime;
    private String endTime;
}
