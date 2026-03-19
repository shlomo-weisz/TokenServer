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

/**
 * Coordinates admin-only workflows such as dashboards, moderation, user maintenance, and manual token adjustments.
 */
@Service
public class AdminService {
    private final UserDao userDao;
    private final AdminDao adminDao;
    private final LessonDao lessonDao;
    private final RatingDao ratingDao;
    private final TokenTransactionDao tokenTransactionDao;
    private final CourseDao courseDao;
    private final TokenService tokenService;
    private final NotificationService notificationService;

    public AdminService(UserDao userDao,
            AdminDao adminDao,
            LessonDao lessonDao,
            RatingDao ratingDao,
            TokenTransactionDao tokenTransactionDao,
            CourseDao courseDao,
            TokenService tokenService,
            NotificationService notificationService) {
        this.userDao = userDao;
        this.adminDao = adminDao;
        this.lessonDao = lessonDao;
        this.ratingDao = ratingDao;
        this.tokenTransactionDao = tokenTransactionDao;
        this.courseDao = courseDao;
        this.tokenService = tokenService;
        this.notificationService = notificationService;
    }

    public void requireAdmin(Integer userId) {
        UserEntity user = requireUser(userId);
        if (!Boolean.TRUE.equals(user.getIsAdmin())) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Admin privileges required");
        }
    }

    public boolean isAdmin(Integer userId) {
        return Boolean.TRUE.equals(requireUser(userId).getIsAdmin());
    }

    public Map<String, Object> dashboard() {
        return Map.of(
                "totalUsers", adminDao.totalUsers(),
                "totalTutors", adminDao.totalTutors(),
                "totalStudents", adminDao.totalStudents(),
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

        boolean resolvedIsAdmin = request.getIsAdmin() == null ? Boolean.TRUE.equals(existing.getIsAdmin()) : Boolean.TRUE.equals(request.getIsAdmin());
        boolean resolvedIsBlockedTutor = request.getIsBlockedTutor() == null
                ? Boolean.TRUE.equals(existing.getIsBlockedTutor())
                : Boolean.TRUE.equals(request.getIsBlockedTutor());
        boolean resolvedIsActive = request.getIsActive() == null ? Boolean.TRUE.equals(existing.getIsActive()) : Boolean.TRUE.equals(request.getIsActive());

        if (Boolean.TRUE.equals(existing.getIsAdmin())
                && adminId.equals(userId)
                && !resolvedIsAdmin) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_OPERATION", "Cannot remove your own admin role");
        }

        String email = resolveRequiredText("email", request.getEmail(), existing.getEmail());
        if (!email.equalsIgnoreCase(existing.getEmail()) && userDao.existsByEmailExcludingUser(email, userId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "EMAIL_EXISTS", "Email already exists");
        }

        String firstName = resolveRequiredText("firstName", request.getFirstName(), existing.getFirstName());
        String lastName = resolveRequiredText("lastName", request.getLastName(), existing.getLastName());
        String phone = resolveOptionalText(request.getPhone(), existing.getPhone());
        String photoUrl = resolveOptionalText(request.getPhotoUrl(), existing.getPhotoUrl());
        String aboutTeacher = resolveOptionalText(request.getAboutMeAsTeacher(), existing.getAboutMeAsTeacher());
        String aboutStudent = resolveOptionalText(request.getAboutMeAsStudent(), existing.getAboutMeAsStudent());

        userDao.updateByAdmin(
                userId,
                email,
                firstName,
                lastName,
                phone,
                photoUrl,
                aboutTeacher,
                aboutStudent,
                resolvedIsAdmin,
                resolvedIsBlockedTutor,
                resolvedIsActive);

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
        String normalizedSubject = subject == null ? "" : subject.trim();
        String normalizedMessage = message == null ? "" : message.trim();
        Long contactId = adminDao.createContact(userId, normalizedSubject, normalizedMessage);
        notificationService.createAdminContactMessageNotification(
                contactId,
                userId,
                userId,
                normalizedSubject,
                normalizedMessage,
                adminDao.findActiveAdminIds());
        return Map.of(
                "id", contactId,
                "status", "submitted",
                "submittedAt", LocalDateTime.now(),
                "actionPath", "/messages?contact=" + contactId);
    }

    public Map<String, Object> contactThread(Integer userId, Long contactId) {
        UserEntity viewer = requireUser(userId);
        AdminContactEntity contact = requireContact(contactId);
        boolean viewerIsAdmin = Boolean.TRUE.equals(viewer.getIsAdmin());
        if (!viewerIsAdmin && !contact.getUserId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the thread owner or admins can access this contact");
        }

        UserEntity owner = userDao.findById(contact.getUserId()).orElse(null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", contact.getContactId());
        out.put("subject", contact.getSubject());
        out.put("status", normalizeContactStatus(contact.getStatus()));
        out.put("submittedAt", contact.getSubmittedAt());
        out.put("ownerId", contact.getUserId());
        out.put("ownerName", formatDisplayName(owner));
        out.put("viewerIsAdmin", viewerIsAdmin);
        out.put("messages", notificationService.adminContactThreadForUser(userId, contactId));
        return out;
    }

    @Transactional
    public Map<String, Object> replyToContact(Integer userId, Long contactId, String message) {
        UserEntity sender = requireUser(userId);
        AdminContactEntity contact = requireContact(contactId);
        boolean senderIsAdmin = Boolean.TRUE.equals(sender.getIsAdmin());
        if (!senderIsAdmin && !contact.getUserId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the thread owner or admins can reply to this contact");
        }

        String normalizedMessage = message == null ? "" : message.trim();
        if (normalizedMessage.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_MESSAGE", "Message is required");
        }

        Long notificationId = notificationService.createAdminContactMessageNotification(
                contactId,
                contact.getUserId(),
                userId,
                contact.getSubject(),
                normalizedMessage,
                adminDao.findActiveAdminIds());
        adminDao.updateContactStatus(contactId, "IN_PROGRESS");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", notificationId);
        out.put("threadId", contactId);
        out.put("message", normalizedMessage);
        out.put("status", "in_progress");
        out.put("sentAt", LocalDateTime.now());
        out.put("actionPath", "/messages?contact=" + contactId);
        return out;
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
            return Map.of(
                    "id", null,
                    "type", "admin_adjustment",
                    "status", "no_op",
                    "userId", userId,
                    "amount", amount,
                    "balanceAfter", unchangedTotal);
        }
        Long txId = tokenTransactionDao.create(TokenTransactionEntity.builder()
                .payerId(userId)
                .receiverId(userId)
                .amount(amount)
                .txType("ADMIN_ADJUST")
                .status("SUCCESS")
                .description(request.getReason() == null ? "Balance adjusted by administrator" : request.getReason())
                .build());
        BigDecimal newTotal = userDao.getBalances(userId).getTotal();
        return Map.of(
                "id", "txn_" + txId,
                "type", "admin_adjustment",
                "status", "succeeded",
                "userId", userId,
                "amount", amount,
                "balanceAfter", newTotal);
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

    private String resolveRequiredText(String fieldName, String incomingValue, String existingValue) {
        if (incomingValue == null) {
            return existingValue;
        }
        String trimmed = incomingValue.trim();
        if (trimmed.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", fieldName + " is required");
        }
        return trimmed;
    }

    private String resolveOptionalText(String incomingValue, String existingValue) {
        if (incomingValue == null) {
            return existingValue;
        }
        return nullableTrim(incomingValue);
    }

    private UserEntity requireUser(Integer userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
    }

    private AdminContactEntity requireContact(Long contactId) {
        return adminDao.findContactById(contactId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Contact thread not found"));
    }

    private String normalizeContactStatus(String status) {
        if (status == null || status.isBlank()) {
            return "submitted";
        }
        return status.trim().toLowerCase();
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
