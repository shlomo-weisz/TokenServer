package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.NotificationDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.LessonRequestEntity;
import com.tokenlearn.server.domain.NotificationEntity;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.util.CourseLabelUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    private final NotificationDao notificationDao;
    private final UserDao userDao;
    private final CourseDao courseDao;

    public NotificationService(NotificationDao notificationDao, UserDao userDao, CourseDao courseDao) {
        this.notificationDao = notificationDao;
        this.userDao = userDao;
        this.courseDao = courseDao;
    }

    public void createLessonRequestApprovedNotification(LessonRequestEntity request, Integer lessonId, LocalDateTime lessonStart) {
        notificationDao.create(NotificationEntity.builder()
                .userId(request.getStudentId())
                .eventType("LESSON_REQUEST_APPROVED")
                .requestId(request.getRequestId())
                .lessonId(lessonId)
                .counterpartName(resolveUserDisplayName(request.getTutorId()))
                .courseName(resolveCourseLabel(request.getCourseId()))
                .scheduledAt(lessonStart)
                .actionPath("/lesson/" + lessonId)
                .isRead(false)
                .build());
    }

    public void createLessonRequestRejectedNotification(LessonRequestEntity request, String rejectionReason) {
        notificationDao.create(NotificationEntity.builder()
                .userId(request.getStudentId())
                .eventType("LESSON_REQUEST_REJECTED")
                .requestId(request.getRequestId())
                .counterpartName(resolveUserDisplayName(request.getTutorId()))
                .courseName(resolveCourseLabel(request.getCourseId()))
                .scheduledAt(request.getSpecificStartTime())
                .rejectionReason(rejectionReason)
                .actionPath("/lesson-requests")
                .isRead(false)
                .build());
    }

    public List<Map<String, Object>> unreadForUser(Integer userId, int limit) {
        return notificationDao.findUnreadByUser(userId, limit).stream()
                .map(this::toPayload)
                .toList();
    }

    public Map<String, Object> markRead(Integer userId, List<Long> ids) {
        int updatedCount = (ids == null || ids.isEmpty())
                ? notificationDao.markAllRead(userId)
                : notificationDao.markRead(userId, ids);
        return Map.of("updatedCount", updatedCount);
    }

    private Map<String, Object> toPayload(NotificationEntity notification) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", notification.getNotificationId());
        out.put("eventType", notification.getEventType());
        out.put("requestId", notification.getRequestId());
        out.put("lessonId", notification.getLessonId());
        out.put("counterpartName", notification.getCounterpartName());
        out.put("courseName", notification.getCourseName());
        out.put("scheduledAt", notification.getScheduledAt());
        out.put("rejectionReason", notification.getRejectionReason());
        out.put("actionPath", notification.getActionPath());
        out.put("isRead", notification.getIsRead());
        out.put("createdAt", notification.getCreatedAt());
        return out;
    }

    private String resolveUserDisplayName(Integer userId) {
        return userDao.findById(userId)
                .map(this::formatDisplayName)
                .orElse("");
    }

    private String resolveCourseLabel(Integer courseId) {
        return courseDao.findById(courseId)
                .map(CourseLabelUtil::buildLabel)
                .orElse("");
    }

    private String formatDisplayName(UserEntity user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getEmail() : fullName;
    }
}
