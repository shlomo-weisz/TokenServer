package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.AdminDao;
import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.LessonDao;
import com.tokenlearn.server.dao.RatingDao;
import com.tokenlearn.server.dao.TokenTransactionDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.AdminContactEntity;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {
    private UserDao userDao;
    private AdminDao adminDao;
    private LessonDao lessonDao;
    private RatingDao ratingDao;
    private CourseDao courseDao;
    private TokenService tokenService;
    private NotificationService notificationService;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        adminDao = mock(AdminDao.class);
        lessonDao = mock(LessonDao.class);
        ratingDao = mock(RatingDao.class);
        TokenTransactionDao tokenTransactionDao = mock(TokenTransactionDao.class);
        courseDao = mock(CourseDao.class);
        tokenService = mock(TokenService.class);
        notificationService = mock(NotificationService.class);

        adminService = new AdminService(
                userDao,
                adminDao,
                lessonDao,
                ratingDao,
                tokenTransactionDao,
                courseDao,
                tokenService,
                notificationService);
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

    @Test
    void contactCreatesSharedAdminThreadAndReturnsThreadLink() {
        when(adminDao.createContact(7, "Billing", "Need help with my payment"))
                .thenReturn(91L);
        when(adminDao.findActiveAdminIds()).thenReturn(List.of(1, 2));

        Map<String, Object> result = adminService.contact(7, " Billing ", " Need help with my payment ");

        verify(notificationService).createAdminContactMessageNotification(
                91L,
                7,
                7,
                "Billing",
                "Need help with my payment",
                List.of(1, 2));
        assertEquals(91L, result.get("contactId"));
        assertEquals("/messages?contact=91", result.get("actionPath"));
        assertEquals("submitted", result.get("status"));
    }

    @Test
    void replyToContactLetsAdminBroadcastReplyToAllAdminsAndOwner() {
        UserEntity admin = user(1, "admin@tokenlearn.com", "Admin", "User", true);
        AdminContactEntity contact = AdminContactEntity.builder()
                .contactId(55L)
                .userId(9)
                .subject("Missing tokens")
                .status("SUBMITTED")
                .build();

        when(userDao.findById(1)).thenReturn(Optional.of(admin));
        when(adminDao.findContactById(55L)).thenReturn(Optional.of(contact));
        when(adminDao.findActiveAdminIds()).thenReturn(List.of(1, 2, 3));
        when(notificationService.createAdminContactMessageNotification(
                55L,
                9,
                1,
                "Missing tokens",
                "We are checking it now",
                List.of(1, 2, 3)))
                .thenReturn(501L);

        Map<String, Object> result = adminService.replyToContact(1, 55L, " We are checking it now ");

        verify(adminDao).updateContactStatus(55L, "IN_PROGRESS");
        assertEquals(501L, result.get("notificationId"));
        assertEquals(55L, result.get("contactId"));
        assertEquals("We are checking it now", result.get("message"));
        assertEquals("/messages?contact=55", result.get("actionPath"));
    }

    @Test
    void replyToContactRejectsUnrelatedNonAdminUser() {
        UserEntity outsider = user(12, "user@tokenlearn.com", "Dana", "Student", false);
        AdminContactEntity contact = AdminContactEntity.builder()
                .contactId(72L)
                .userId(9)
                .subject("Support")
                .build();

        when(userDao.findById(12)).thenReturn(Optional.of(outsider));
        when(adminDao.findContactById(72L)).thenReturn(Optional.of(contact));

        assertThrows(com.tokenlearn.server.exception.AppException.class, () -> adminService.replyToContact(12, 72L, "Hello"));
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
