package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.AvailabilityDao;
import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.RatingDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.AvailabilityEntity;
import com.tokenlearn.server.domain.CourseEntity;
import com.tokenlearn.server.domain.RatingEntity;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.dto.*;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.util.CourseLabelUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserDao userDao;
    private final CourseDao courseDao;
    private final AvailabilityDao availabilityDao;
    private final RatingDao ratingDao;

    public UserService(UserDao userDao, CourseDao courseDao, AvailabilityDao availabilityDao, RatingDao ratingDao) {
        this.userDao = userDao;
        this.courseDao = courseDao;
        this.availabilityDao = availabilityDao;
        this.ratingDao = ratingDao;
    }

    public UserEntity requireUser(Integer userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
    }

    public UserProfileDto getProfile(Integer userId, boolean includeSecretQuestion) {
        UserEntity user = requireUser(userId);
        TokenBalancesDto balances = userDao.getBalances(userId);
        BigDecimal rating = ratingDao.averageForUser(userId);
        int totalLessonsAsTutor = 0;
        List<SimpleCourseDto> teacherCourses = toSimpleCourses(courseDao.findTeacherCourses(userId));
        List<SimpleCourseDto> studentCourses = toSimpleCourses(courseDao.findStudentCourses(userId));

        return UserProfileDto.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .photoUrl(user.getPhotoUrl())
                .isAdmin(user.getIsAdmin())
                .tokenBalance(balances.getTotal())
                .tokenBalances(balances)
                .tutorRating(rating)
                .totalLessonsAsTutor(totalLessonsAsTutor)
                .coursesAsTeacher(teacherCourses)
                .coursesAsStudent(studentCourses)
                .availabilityAsTeacher(toAvailabilityDtos(availabilityDao.findByUserAndRole(userId, "teacher"), true))
                .availabilityAsStudent(toAvailabilityDtos(availabilityDao.findByUserAndRole(userId, "student"), true))
                .aboutMeAsTeacher(user.getAboutMeAsTeacher())
                .aboutMeAsStudent(user.getAboutMeAsStudent())
                .secretQuestion(includeSecretQuestion ? user.getSecretQuestion() : null)
                .build();
    }

    @Transactional
    public UserProfileDto updateProfile(Integer userId, UpdateUserProfileRequest request) {
        userDao.updateProfile(
                userId,
                request.getFirstName(),
                request.getLastName(),
                request.getPhone(),
                request.getPhotoUrl(),
                request.getAboutMeAsTeacher(),
                request.getAboutMeAsStudent());

        if (request.getCoursesAsTeacher() != null) {
            List<Integer> ids = resolveCourseIds(request.getCoursesAsTeacher());
            courseDao.replaceTeacherCourses(userId, ids);
        }
        if (request.getCoursesAsStudent() != null) {
            List<Integer> ids = resolveCourseIds(request.getCoursesAsStudent());
            courseDao.replaceStudentCourses(userId, ids);
        }
        if (request.getAvailabilityAsTeacher() != null) {
            availabilityDao.replaceForUserRole(userId, "teacher", toAvailabilityEntities(userId, "teacher", request.getAvailabilityAsTeacher()));
        }
        if (request.getAvailabilityAsStudent() != null) {
            availabilityDao.replaceForUserRole(userId, "student", toAvailabilityEntities(userId, "student", request.getAvailabilityAsStudent()));
        }
        return getProfile(userId, true);
    }

    @Transactional
    public String updatePhoto(Integer userId, String originalFilename) {
        String safe = originalFilename == null ? "photo.jpg" : originalFilename.replaceAll("[^a-zA-Z0-9_.-]", "_");
        String url = "https://tokenlearn.local/uploads/" + userId + "/" + System.currentTimeMillis() + "_" + safe;
        userDao.updatePhoto(userId, url);
        return url;
    }

    public TokenBalancesDto getBalances(Integer userId) {
        return userDao.getBalances(userId);
    }

    public Map<String, Object> getUserRatings(Integer userId) {
        BigDecimal avg = ratingDao.averageForUser(userId);
        int total = ratingDao.countForUser(userId);
        List<Map<String, Object>> items = ratingDao.findForUser(userId).stream().map(this::toRatingItem).toList();
        return Map.of(
                "averageRating", avg,
                "totalRatings", total,
                "ratings", items);
    }

    private Map<String, Object> toRatingItem(RatingEntity rating) {
        UserEntity from = requireUser(rating.getFromUserId());
        return Map.of(
                "ratedBy", from.getFirstName() + " " + from.getLastName(),
                "rating", rating.getScore(),
                "comment", rating.getComment() == null ? "" : rating.getComment());
    }

    private List<SimpleCourseDto> toSimpleCourses(List<CourseEntity> courses) {
        return courses.stream()
                .map(c -> SimpleCourseDto.builder()
                        .id(c.getCourseId())
                        .courseNumber(c.getCourseNumber())
                        .nameHe(c.getNameHe())
                        .nameEn(c.getNameEn())
                        .name(CourseLabelUtil.buildLabel(c))
                        .build())
                .collect(Collectors.toList());
    }

    private List<Integer> resolveCourseIds(List<CourseSelectionDto> input) {
        List<Integer> ids = new ArrayList<>();
        for (CourseSelectionDto c : input) {
            if (c.getId() != null) {
                courseDao.findById(c.getId())
                        .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "INVALID_COURSE", "Unknown course id: " + c.getId()));
                ids.add(c.getId());
            } else {
                String[] candidates = new String[] { c.getCourseNumber(), c.getNameHe(), c.getNameEn(), c.getName() };
                Integer resolvedId = null;
                String attemptedValue = null;
                for (String candidate : candidates) {
                    if (candidate == null || candidate.isBlank()) {
                        continue;
                    }
                    attemptedValue = candidate;
                    resolvedId = courseDao.findByIdentifier(candidate).map(CourseEntity::getCourseId).orElse(null);
                    if (resolvedId != null) {
                        break;
                    }
                }
                if (resolvedId != null) {
                    ids.add(resolvedId);
                } else if (attemptedValue != null) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_COURSE", "Unknown course: " + attemptedValue);
                }
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private List<AvailabilityEntity> toAvailabilityEntities(Integer userId, String role, List<AvailabilityInputDto> input) {
        List<AvailabilityEntity> result = new ArrayList<>();
        for (AvailabilityInputDto dto : input) {
            try {
                result.add(AvailabilityEntity.builder()
                        .userId(userId)
                        .role(role)
                        .day(dto.getDay())
                        .startTime(LocalTime.parse(dto.getStartTime()))
                        .endTime(LocalTime.parse(dto.getEndTime()))
                        .build());
            } catch (DateTimeParseException ex) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TIME", "Invalid availability time format");
            }
        }
        return result;
    }

    private List<AvailabilityDto> toAvailabilityDtos(List<AvailabilityEntity> entities, boolean includeIsAvailable) {
        return entities.stream().map(a -> AvailabilityDto.builder()
                .id(a.getAvailabilityId())
                .day(a.getDay())
                .startTime(a.getStartTime().toString())
                .endTime(a.getEndTime().toString())
                .isAvailable(includeIsAvailable ? true : null)
                .build()).toList();
    }
}
