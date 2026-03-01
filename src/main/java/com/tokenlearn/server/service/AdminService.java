package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.AdminDao;
import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.LessonDao;
import com.tokenlearn.server.dao.RatingDao;
import com.tokenlearn.server.dao.TokenTransactionDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.LessonEntity;
import com.tokenlearn.server.domain.TokenTransactionEntity;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.dto.UpdateUserTokensRequest;
import com.tokenlearn.server.exception.AppException;
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

    public AdminService(UserDao userDao,
            AdminDao adminDao,
            LessonDao lessonDao,
            RatingDao ratingDao,
            TokenTransactionDao tokenTransactionDao,
            CourseDao courseDao) {
        this.userDao = userDao;
        this.adminDao = adminDao;
        this.lessonDao = lessonDao;
        this.ratingDao = ratingDao;
        this.tokenTransactionDao = tokenTransactionDao;
        this.courseDao = courseDao;
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
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", u.getUserId());
            out.put("email", u.getEmail());
            out.put("firstName", u.getFirstName());
            out.put("lastName", u.getLastName());
            out.put("phone", u.getPhone() == null ? "" : u.getPhone());
            out.put("isAdmin", u.getIsAdmin());
            out.put("blockedTutor", u.getIsBlockedTutor());
            out.put("available", u.getAvailableBalance());
            out.put("locked", u.getLockedBalance());
            return out;
        }).toList();
    }

    public Map<String, Object> statistics() {
        return Map.of(
                "lessonsThisMonth", adminDao.lessonsThisMonth(),
                "lessonsThisWeek", adminDao.lessonsThisWeek(),
                "averageRating", ratingDao.globalAverage(),
                "mostPopularCourses", adminDao.mostPopularCourses(5).stream().map(c -> c.getName()).toList());
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

    @Transactional
    public Map<String, Object> adjustTokens(Integer userId, UpdateUserTokensRequest request) {
        UserEntity user = userDao.findById(userId).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount cannot be zero");
        }
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            userDao.addAvailable(userId, amount);
        } else {
            boolean ok = userDao.subtractAvailable(userId, amount.abs());
            if (!ok) {
                throw new AppException(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_BALANCE", "Insufficient available balance for deduction");
            }
        }
        tokenTransactionDao.create(TokenTransactionEntity.builder()
                .payerId(userId)
                .receiverId(userId)
                .amount(amount.abs())
                .txType("ADMIN_ADJUST")
                .status("SUCCESS")
                .description(request.getReason() == null ? "Admin adjustment" : request.getReason())
                .build());
        BigDecimal newTotal = userDao.getBalances(userId).getTotal();
        return Map.of("userId", userId, "newBalance", newTotal, "adjustment", amount);
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
        String course = courseDao.findById(l.getCourseId()).map(c -> c.getName()).orElse("");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", l.getLessonId());
        out.put("studentId", l.getStudentId());
        out.put("studentName", student == null ? "" : student.getFirstName() + " " + student.getLastName());
        out.put("tutorId", l.getTutorId());
        out.put("tutorName", tutor == null ? "" : tutor.getFirstName() + " " + tutor.getLastName());
        out.put("course", course);
        out.put("startTime", l.getStartTime());
        out.put("endTime", l.getEndTime());
        out.put("status", l.getStatus().toLowerCase());
        return out;
    }
}
