package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.AdminDao;
import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.LessonDao;
import com.tokenlearn.server.dao.RatingDao;
import com.tokenlearn.server.dao.TokenTransactionDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.CourseEntity;
import com.tokenlearn.server.domain.LessonEntity;
import com.tokenlearn.server.domain.RatingEntity;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.dto.AdminUpdateRatingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {
    private UserDao userDao;
    private LessonDao lessonDao;
    private RatingDao ratingDao;
    private CourseDao courseDao;
    private TokenService tokenService;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        AdminDao adminDao = mock(AdminDao.class);
        lessonDao = mock(LessonDao.class);
        ratingDao = mock(RatingDao.class);
        TokenTransactionDao tokenTransactionDao = mock(TokenTransactionDao.class);
        courseDao = mock(CourseDao.class);
        tokenService = mock(TokenService.class);

        adminService = new AdminService(
                userDao,
                adminDao,
                lessonDao,
                ratingDao,
                tokenTransactionDao,
                courseDao,
                tokenService);
    }

    @Test
    void listRatingsReturnsRatingMetadataForAdmin() {
        UserEntity admin = user(1, "admin@tokenlearn.com", "Admin", "User", true);
        UserEntity student = user(2, "student@tokenlearn.com", "Dana", "Student", false);
        UserEntity tutor = user(3, "tutor@tokenlearn.com", "Ron", "Tutor", false);
        LessonEntity lesson = LessonEntity.builder()
                .lessonId(41)
                .studentId(2)
                .tutorId(3)
                .courseId(7001)
                .startTime(LocalDateTime.of(2026, 3, 8, 10, 0))
                .endTime(LocalDateTime.of(2026, 3, 8, 11, 0))
                .status("COMPLETED")
                .build();
        RatingEntity rating = RatingEntity.builder()
                .ratingId(55)
                .lessonId(41)
                .fromUserId(2)
                .toUserId(3)
                .score(new BigDecimal("4.50"))
                .comment("Very helpful")
                .createdAt(LocalDateTime.of(2026, 3, 8, 12, 0))
                .build();
        CourseEntity course = CourseEntity.builder()
                .courseId(7001)
                .courseNumber("20480")
                .nameHe("מבני נתונים")
                .nameEn("Data Structures")
                .build();

        when(userDao.findById(1)).thenReturn(Optional.of(admin));
        when(ratingDao.findAll(100, 0)).thenReturn(List.of(rating));
        when(ratingDao.countAll()).thenReturn(1);
        when(lessonDao.findById(41)).thenReturn(Optional.of(lesson));
        when(userDao.findById(2)).thenReturn(Optional.of(student));
        when(userDao.findById(3)).thenReturn(Optional.of(tutor));
        when(courseDao.findById(7001)).thenReturn(Optional.of(course));

        adminService.requireAdmin(1);
        Map<String, Object> result = adminService.listRatings(100, 0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ratings = (List<Map<String, Object>>) result.get("ratings");
        Map<String, Object> item = ratings.get(0);

        assertEquals(1, result.get("totalCount"));
        assertEquals(55, item.get("id"));
        assertEquals("Dana Student", item.get("fromUserName"));
        assertEquals("Ron Tutor", item.get("toUserName"));
        assertEquals("Dana Student", item.get("studentName"));
        assertEquals("Ron Tutor", item.get("tutorName"));
        assertEquals("20480 - מבני נתונים", item.get("courseLabel"));
        assertEquals("Very helpful", item.get("comment"));
        assertEquals("completed", item.get("lessonStatus"));
    }

    @Test
    void updateRatingAllowsAdminToEditScoreAndComment() {
        UserEntity admin = user(1, "admin@tokenlearn.com", "Admin", "User", true);
        UserEntity student = user(2, "student@tokenlearn.com", "Dana", "Student", false);
        UserEntity tutor = user(3, "tutor@tokenlearn.com", "Ron", "Tutor", false);
        LessonEntity lesson = LessonEntity.builder()
                .lessonId(41)
                .studentId(2)
                .tutorId(3)
                .courseId(null)
                .startTime(LocalDateTime.of(2026, 3, 8, 10, 0))
                .endTime(LocalDateTime.of(2026, 3, 8, 11, 0))
                .status("COMPLETED")
                .build();
        RatingEntity existing = RatingEntity.builder()
                .ratingId(55)
                .lessonId(41)
                .fromUserId(2)
                .toUserId(3)
                .score(new BigDecimal("4.00"))
                .comment("Old")
                .createdAt(LocalDateTime.of(2026, 3, 8, 12, 0))
                .build();
        RatingEntity updated = RatingEntity.builder()
                .ratingId(55)
                .lessonId(41)
                .fromUserId(2)
                .toUserId(3)
                .score(new BigDecimal("5.00"))
                .comment("Updated by admin")
                .createdAt(LocalDateTime.of(2026, 3, 8, 12, 0))
                .build();
        AdminUpdateRatingRequest request = new AdminUpdateRatingRequest();
        request.setRating(new BigDecimal("5.00"));
        request.setComment(" Updated by admin ");

        when(userDao.findById(1)).thenReturn(Optional.of(admin));
        when(ratingDao.findById(55)).thenReturn(Optional.of(existing), Optional.of(updated));
        when(ratingDao.update(any(), any(), any())).thenReturn(1);
        when(lessonDao.findById(41)).thenReturn(Optional.of(lesson));
        when(userDao.findById(2)).thenReturn(Optional.of(student));
        when(userDao.findById(3)).thenReturn(Optional.of(tutor));

        Map<String, Object> result = adminService.updateRating(1, 55, request);

        verify(ratingDao).update(55, new BigDecimal("5.00"), "Updated by admin");
        assertEquals("5.00", String.valueOf(result.get("rating")));
        assertEquals("Updated by admin", result.get("comment"));
        assertNull(result.get("courseId"));
    }

    @Test
    void userTokenHistoryReturnsTargetUserAuditTrailForAdmin() {
        UserEntity admin = user(1, "admin@tokenlearn.com", "Admin", "User", true);
        UserEntity target = user(9, "student@tokenlearn.com", "Dana", "Student", false);

        when(userDao.findById(1)).thenReturn(Optional.of(admin));
        when(userDao.findById(9)).thenReturn(Optional.of(target));
        when(tokenService.history(9, 25, 0)).thenReturn(Map.of(
                "transactions", List.of(Map.of("id", "txn_10", "reason", "Balance adjusted by administrator")),
                "totalCount", 1));

        Map<String, Object> result = adminService.userTokenHistory(1, 9, 25, 0);

        assertEquals(9, result.get("userId"));
        assertEquals("student@tokenlearn.com", result.get("email"));
        assertEquals("Dana Student", result.get("fullName"));
        assertEquals(1, result.get("totalCount"));
        verify(tokenService).history(9, 25, 0);
    }

    private UserEntity user(Integer id, String email, String firstName, String lastName, boolean isAdmin) {
        return UserEntity.builder()
                .userId(id)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .isAdmin(isAdmin)
                .build();
    }
}
