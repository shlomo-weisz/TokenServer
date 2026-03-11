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
import com.tokenlearn.server.domain.TokenTransactionEntity;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.dto.AdminUpdateRatingRequest;
import com.tokenlearn.server.dto.AdminUpdateUserRequest;
import com.tokenlearn.server.dto.SimpleCourseDto;
import com.tokenlearn.server.dto.UpdateUserTokensRequest;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.util.CourseLabelUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final UserDao userDao;
    private final AdminDao adminDao;
    private final LessonDao lessonDao;
    private final RatingDao ratingDao;
    private final TokenTransactionDao tokenTransactionDao;
    private final CourseDao courseDao;
    private final TokenService tokenService;

    public AdminService(UserDao userDao,
            AdminDao adminDao,
            LessonDao lessonDao,
            RatingDao ratingDao,
            TokenTransactionDao tokenTransactionDao,
            CourseDao courseDao,
            TokenService tokenService) {
        this.userDao = userDao;
        this.adminDao = adminDao;
        this.lessonDao = lessonDao;
        this.ratingDao = ratingDao;
        this.tokenTransactionDao = tokenTransactionDao;
        this.courseDao = courseDao;
        this.tokenService = tokenService;
    }

    public void requireAdmin(Integer userId) {
        UserEntity user = userDao.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
        if (!Boolean.TRUE.equals(user.getIsAdmin())) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Admin privileges required");
        }
    }

    public Map<String, Object> dashboard() {
        return Map.of(
                "totalUsers", adminDao.totalUsers(),
                "totalLessons", adminDao.totalLessons(),
                "totalRequests", adminDao.totalRequests(),
                "pendingRequests", adminDao.pendingRequests(),
                "recentActivity", adminDao.recentActivity(10).stream().map(row -> Map.of(
                        "type", row.get("type"),
                        "user", "system",
                        "timestamp", row.get("event_time"))).toList());
    }

    public List<Map<String, Object>> users(int limit, int offset, String role) {
        return userDao.listUsers(limit, offset).stream().filter(u -> matchRole(u.getUserId(), role)).map(u -> {
            List<SimpleCourseDto> coursesAsTeacher = toSimpleCourses(courseDao.findTeacherCourses(u.getUserId()));
            List<SimpleCourseDto> coursesAsStudent = toSimpleCourses(courseDao.findStudentCourses(u.getUserId()));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", u.getUserId());
            out.put("email", u.getEmail());
            out.put("firstName", u.getFirstName());
            out.put("lastName", u.getLastName());
            out.put("phone", u.getPhone() == null ? "" : u.getPhone());
            out.put("isAdmin", u.getIsAdmin());
            out.put("isActive", u.getIsActive());
            out.put("blockedTutor", u.getIsBlockedTutor());
            out.put("photoUrl", u.getPhotoUrl());
            out.put("aboutMeAsTeacher", u.getAboutMeAsTeacher());
            out.put("aboutMeAsStudent", u.getAboutMeAsStudent());
            out.put("available", u.getAvailableBalance());
            out.put("locked", u.getLockedBalance());
            out.put("tokenBalance", u.getAvailableBalance().add(u.getLockedBalance()));
            out.put("tutorRating", ratingDao.averageForUser(u.getUserId()));
            out.put("coursesAsTeacher", coursesAsTeacher);
            out.put("coursesAsStudent", coursesAsStudent);
            return out;
        }).toList();
    }

    @Transactional
    public Map<String, Object> updateUser(Integer adminId, Integer userId, AdminUpdateUserRequest request) {
        requireAdmin(adminId);
        UserEntity existing = userDao.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        if (Boolean.TRUE.equals(existing.getIsAdmin())
                && adminId.equals(userId)
                && Boolean.FALSE.equals(request.getIsAdmin())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_OPERATION", "Cannot remove your own admin role");
        }

        String email = request.getEmail().trim();
        if (userDao.existsByEmailExcludingUser(email, userId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "EMAIL_EXISTS", "Email already exists");
        }

        userDao.updateByAdmin(
                userId,
                email,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                nullableTrim(request.getPhone()),
                nullableTrim(request.getPhotoUrl()),
                nullableTrim(request.getAboutMeAsTeacher()),
                nullableTrim(request.getAboutMeAsStudent()),
                Boolean.TRUE.equals(request.getIsAdmin()),
                Boolean.TRUE.equals(request.getIsBlockedTutor()),
                Boolean.TRUE.equals(request.getIsActive()));

        UserEntity updated = userDao.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", updated.getUserId());
        out.put("email", updated.getEmail());
        out.put("firstName", updated.getFirstName());
        out.put("lastName", updated.getLastName());
        out.put("phone", updated.getPhone() == null ? "" : updated.getPhone());
        out.put("isAdmin", updated.getIsAdmin());
        out.put("isActive", updated.getIsActive());
        out.put("blockedTutor", updated.getIsBlockedTutor());
        out.put("photoUrl", updated.getPhotoUrl());
        out.put("aboutMeAsTeacher", updated.getAboutMeAsTeacher());
        out.put("aboutMeAsStudent", updated.getAboutMeAsStudent());
        out.put("available", updated.getAvailableBalance());
        out.put("locked", updated.getLockedBalance());
        out.put("tokenBalance", updated.getAvailableBalance().add(updated.getLockedBalance()));
        out.put("tutorRating", ratingDao.averageForUser(updated.getUserId()));
        out.put("coursesAsTeacher", toSimpleCourses(courseDao.findTeacherCourses(updated.getUserId())));
        out.put("coursesAsStudent", toSimpleCourses(courseDao.findStudentCourses(updated.getUserId())));
        return out;
    }

    @Transactional
    public Map<String, Object> deleteUser(Integer adminId, Integer userId) {
        requireAdmin(adminId);
        if (adminId.equals(userId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_OPERATION", "Cannot delete your own account");
        }

        UserEntity user = userDao.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        int deleted = userDao.hardDeleteUser(userId, user.getEmail());
        if (deleted != 1) {
            throw new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found");
        }

        return Map.of(
                "deleted", true,
                "userId", userId,
                "email", user.getEmail());
    }

    public Map<String, Object> statistics() {
        List<CourseEntity> popularCourses = adminDao.mostPopularCourses(5);
        return Map.of(
                "lessonsThisMonth", adminDao.lessonsThisMonth(),
                "lessonsThisWeek", adminDao.lessonsThisWeek(),
                "averageRating", ratingDao.globalAverage(),
                "mostPopularCourses", popularCourses.stream().map(CourseLabelUtil::buildLabel).toList(),
                "mostPopularCourseOptions", toSimpleCourses(popularCourses));
    }

    @Transactional
    public Map<String, Object> contact(Integer userId, String subject, String message) {
        Long id = adminDao.createContact(userId, subject, message);
        return Map.of(
                "id", "contact_" + id,
                "status", "submitted",
                "submittedAt", LocalDateTime.now());
    }

    @Transactional
    public Map<String, Object> setTutorBlocked(Integer tutorId, boolean blocked) {
        userDao.findById(tutorId).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Tutor not found"));
        userDao.setTutorBlocked(tutorId, blocked);
        return Map.of("tutorId", tutorId, "blocked", blocked);
    }

    public Map<String, Object> listLessons(String status, int limit, int offset) {
        List<Map<String, Object>> lessons = lessonDao.findAllForAdmin(status == null ? null : status.toUpperCase(), limit, offset)
                .stream().map(this::toAdminLesson).toList();
        return Map.of("lessons", lessons, "totalCount", lessonDao.countAllForAdmin(status == null ? null : status.toUpperCase()));
    }

    public Map<String, Object> listRatings(int limit, int offset) {
        List<Map<String, Object>> ratings = ratingDao.findAll(limit, offset).stream()
                .map(this::toAdminRating)
                .toList();
        return Map.of(
                "ratings", ratings,
                "totalCount", ratingDao.countAll());
    }

    @Transactional
    public Map<String, Object> updateRating(Integer adminId, Integer ratingId, AdminUpdateRatingRequest request) {
        requireAdmin(adminId);
        ratingDao.findById(ratingId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Rating not found"));

        int updatedCount = ratingDao.update(ratingId, request.getRating(), nullableTrim(request.getComment()));
        if (updatedCount != 1) {
            throw new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Rating not found");
        }

        RatingEntity updated = ratingDao.findById(ratingId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Rating not found"));
        return toAdminRating(updated);
    }

    @Transactional
    public Map<String, Object> adjustTokens(Integer userId, UpdateUserTokensRequest request) {
        UserEntity user = userDao.findById(userId).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            userDao.addAvailable(userId, amount);
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            boolean ok = userDao.subtractAvailable(userId, amount.abs());
            if (!ok) {
                throw new AppException(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_BALANCE", "Insufficient available balance for deduction");
            }
        } else {
            BigDecimal unchangedTotal = userDao.getBalances(userId).getTotal();
            return Map.of("userId", userId, "newBalance", unchangedTotal, "adjustment", amount, "noOp", true);
        }
        tokenTransactionDao.create(TokenTransactionEntity.builder()
                .payerId(userId)
                .receiverId(userId)
                .amount(amount)
                .txType("ADMIN_ADJUST")
                .status("SUCCESS")
                .description(request.getReason() == null ? "Balance adjusted by administrator" : request.getReason())
                .build());
        BigDecimal newTotal = userDao.getBalances(userId).getTotal();
        return Map.of("userId", userId, "newBalance", newTotal, "adjustment", amount);
    }

    public Map<String, Object> userTokenHistory(Integer adminId, Integer userId, int limit, int offset) {
        requireAdmin(adminId);
        UserEntity targetUser = userDao.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        Map<String, Object> history = new LinkedHashMap<>(tokenService.history(userId, limit, offset));
        history.put("userId", targetUser.getUserId());
        history.put("email", targetUser.getEmail());
        history.put("fullName", formatDisplayName(targetUser));
        return history;
    }

    private boolean matchRole(Integer userId, String role) {
        if (role == null || role.isBlank()) {
            return true;
        }
        if ("teacher".equalsIgnoreCase(role)) {
            return !courseDao.findTeacherCourses(userId).isEmpty();
        }
        if ("student".equalsIgnoreCase(role)) {
            return !courseDao.findStudentCourses(userId).isEmpty();
        }
        return true;
    }

    private Map<String, Object> toAdminLesson(LessonEntity l) {
        UserEntity student = userDao.findById(l.getStudentId()).orElse(null);
        UserEntity tutor = userDao.findById(l.getTutorId()).orElse(null);
        CourseEntity course = l.getCourseId() == null ? null : courseDao.findById(l.getCourseId()).orElse(null);
        String courseLabel = course == null ? "" : CourseLabelUtil.buildLabel(course);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", l.getLessonId());
        out.put("studentId", l.getStudentId());
        out.put("studentName", student == null ? "" : student.getFirstName() + " " + student.getLastName());
        out.put("tutorId", l.getTutorId());
        out.put("tutorName", tutor == null ? "" : tutor.getFirstName() + " " + tutor.getLastName());
        out.put("course", courseLabel);
        out.put("courseLabel", courseLabel);
        out.put("courseId", course == null ? null : course.getCourseId());
        out.put("courseNumber", course == null ? null : course.getCourseNumber());
        out.put("courseNameHe", course == null ? null : course.getNameHe());
        out.put("courseNameEn", course == null ? null : course.getNameEn());
        out.put("startTime", l.getStartTime());
        out.put("endTime", l.getEndTime());
        out.put("status", l.getStatus().toLowerCase());
        return out;
    }

    private Map<String, Object> toAdminRating(RatingEntity rating) {
        LessonEntity lesson = lessonDao.findById(rating.getLessonId()).orElse(null);
        UserEntity fromUser = userDao.findById(rating.getFromUserId()).orElse(null);
        UserEntity toUser = userDao.findById(rating.getToUserId()).orElse(null);
        UserEntity student = lesson == null ? null : userDao.findById(lesson.getStudentId()).orElse(null);
        UserEntity tutor = lesson == null ? null : userDao.findById(lesson.getTutorId()).orElse(null);
        CourseEntity course = lesson == null || lesson.getCourseId() == null
                ? null
                : courseDao.findById(lesson.getCourseId()).orElse(null);
        String courseLabel = course == null ? "" : CourseLabelUtil.buildLabel(course);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", rating.getRatingId());
        out.put("lessonId", rating.getLessonId());
        out.put("fromUserId", rating.getFromUserId());
        out.put("fromUserName", formatDisplayName(fromUser));
        out.put("toUserId", rating.getToUserId());
        out.put("toUserName", formatDisplayName(toUser));
        out.put("studentId", lesson == null ? null : lesson.getStudentId());
        out.put("studentName", formatDisplayName(student));
        out.put("tutorId", lesson == null ? null : lesson.getTutorId());
        out.put("tutorName", formatDisplayName(tutor));
        out.put("course", courseLabel);
        out.put("courseLabel", courseLabel);
        out.put("courseId", course == null ? null : course.getCourseId());
        out.put("courseNumber", course == null ? null : course.getCourseNumber());
        out.put("courseNameHe", course == null ? null : course.getNameHe());
        out.put("courseNameEn", course == null ? null : course.getNameEn());
        out.put("rating", rating.getScore());
        out.put("comment", rating.getComment() == null ? "" : rating.getComment());
        out.put("createdAt", rating.getCreatedAt());
        out.put("lessonStartTime", lesson == null ? null : lesson.getStartTime());
        out.put("lessonEndTime", lesson == null ? null : lesson.getEndTime());
        out.put("lessonStatus", lesson == null || lesson.getStatus() == null ? null : lesson.getStatus().toLowerCase());
        return out;
    }

    private List<SimpleCourseDto> toSimpleCourses(List<CourseEntity> courses) {
        return courses.stream()
                .map(course -> SimpleCourseDto.builder()
                        .id(course.getCourseId())
                        .courseNumber(course.getCourseNumber())
                        .nameHe(course.getNameHe())
                        .nameEn(course.getNameEn())
                        .name(CourseLabelUtil.buildLabel(course))
                        .build())
                .toList();
    }

    private String nullableTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatDisplayName(UserEntity user) {
        if (user == null) {
            return "";
        }
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? (user.getEmail() == null ? "" : user.getEmail()) : fullName;
    }
}
