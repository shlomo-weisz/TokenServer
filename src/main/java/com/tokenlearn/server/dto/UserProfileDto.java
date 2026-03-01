package com.tokenlearn.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private Integer id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String photoUrl;
    private Boolean isAdmin;
    private BigDecimal tokenBalance;
    private TokenBalancesDto tokenBalances;
    private BigDecimal tutorRating;
    private Integer totalLessonsAsTutor;
    private List<SimpleCourseDto> coursesAsTeacher;
    private List<SimpleCourseDto> coursesAsStudent;
    private List<AvailabilityDto> availabilityAsTeacher;
    private List<AvailabilityDto> availabilityAsStudent;
    private String aboutMeAsTeacher;
    private String aboutMeAsStudent;
    private String secretQuestion;
}
