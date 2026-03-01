package com.tokenlearn.server.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String photoUrl;
    private List<CourseSelectionDto> coursesAsTeacher;
    private List<CourseSelectionDto> coursesAsStudent;
    private List<AvailabilityInputDto> availabilityAsTeacher;
    private List<AvailabilityInputDto> availabilityAsStudent;
    private String aboutMeAsTeacher;
    private String aboutMeAsStudent;
}
