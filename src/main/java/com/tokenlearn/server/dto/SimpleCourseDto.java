package com.tokenlearn.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compact course descriptor embedded inside larger API payloads.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleCourseDto {
    private Integer id;
    private String courseNumber;
    private String nameHe;
    private String nameEn;
    private String name;
}
