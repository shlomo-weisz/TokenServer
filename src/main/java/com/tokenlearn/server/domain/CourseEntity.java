package com.tokenlearn.server.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEntity {
    private Integer courseId;
    private String name;
    private String category;
    private Boolean isActive;
}
