package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.LessonDao;
import com.tokenlearn.server.dao.NotificationDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.LessonEntity;
import com.tokenlearn.server.domain.LessonRequestEntity;
import com.tokenlearn.server.domain.NotificationEntity;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.util.CourseLabelUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Persists inbox notifications consumed by the client.
 *
 * <p>The notification table acts as a durable inbox for request status changes,
 * reminders, and lesson chat messages.
 */
@Service
public class NotificationService {
    public static final String EVENT_LESSON_REQUEST_CREATED = "LESSON_REQUEST_CREATED";
    public static final String EVENT_LESSON_REQUEST_APPROVED = "LESSON_REQUEST_APPROVED";
    public static final String EVENT_LESSON_REQUEST_REJECTED = "LESSON_REQUEST_REJECTED";
    public static final String EVENT_LESSON_CANCELLED = "LESSON_CANCELLED";
    public static final String EVENT_LESSON_REMINDER = "LESSON_REMINDER";
    public static final String EVENT_LESSON_MESSAGE = "LESSON_MESSAGE";
    public static final String EVENT_ADMIN_CONTACT_MESSAGE = "ADMIN_CONTACT_MESSAGE";
    private static final String MANAGER_TEAM_LABEL = "TokenLearn Managers";

    private final NotificationDao notificationDao;
    private final UserDao userDao;
    private final CourseDao courseDao;
    private final LessonDao lessonDao;

    public NotificationService(NotificationDao notificationDao, UserDao userDao, CourseDao courseDao, LessonDao lessonDao) {
        this.notificationDao = notificationDao;
        this.userDao = userDao;
        this.courseDao = courseDao;
        this.lessonDao = lessonDao;
    }

    public void createLessonRequestCreatedNotification(LessonRequestEntity request) {
        notificationDao.create(NotificationEntity.builder()
                .userId(request.getTutorId())
                .eventType(EVENT_LESSON_REQUEST_CREATED)
                .requestId(request.getRequestId())
                .counterpartName(resolveUserDisplayName(request.getStudentId()))
                .courseName(resolveCourseLabel(request.getCourseId()))
                .scheduledAt(request.getSpecificStartTime())
                .actionPath("/lesson-requests")
                .isRead(false)
                .build());
    }

    public void createLessonRequestApprovedNotification(LessonRequestEntity request, Integer lessonId, LocalDateTime lessonStart) {
        notificationDao.create(NotificationEntity.builder()
                .userId(request.getStudentId())
                .eventType(EVENT_LESSON_REQUEST_APPROVED)
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
                .eventType(EVENT_LESSON_REQUEST_REJECTED)
                .requestId(request.getRequestId())
                .counterpartName(resolveUserDisplayName(request.getTutorId()))
                .courseName(resolveCourseLabel(request.getCourseId()))
                .scheduledAt(request.getSpecificStartTime())
                .rejectionReason(rejectionReason)
                .actionPath("/lesson-requests")
                .isRead(false)
                .build());
    }

    public void createLessonCancelledNotification(LessonEntity lesson, LessonRequestEntity request, Integer actorId, String reason) {
        Integer recipientId = actorId.equals(lesson.getStudentId()) ? lesson.getTutorId() : lesson.getStudentId();
        Integer counterpartId = actorId;
        notificationDao.create(NotificationEntity.builder()
                .userId(recipientId)
                .eventType(EVENT_LESSON_CANCELLED)
                .requestId(request.getRequestId())
                .lessonId(lesson.getLessonId())
                .counterpartName(resolveUserDisplayName(counterpartId))
                .courseName(resolveCourseLabel(lesson.getCourseId()))
                .scheduledAt(lesson.getStartTime())
                .rejectionReason(reason)
                .actionPath("/lesson/" + lesson.getLessonId())
                .isRead(false)
                .build());
    }

    public Long createLessonMessageNotification(LessonEntity lesson, Integer senderId, String messageBody) {
        Integer recipientId = senderId.equals(lesson.getStudentId()) ? lesson.getTutorId() : lesson.getStudentId();
        String recipientName = resolveUserDisplayName(recipientId);
        String senderName = resolveUserDisplayName(senderId);
        String courseName = resolveCourseLabel(lesson.getCourseId());

        Long recipientNotificationId = notificationDao.create(NotificationEntity.builder()
                .userId(recipientId)
                .eventType(EVENT_LESSON_MESSAGE)
                .requestId(lesson.getRequestId())
                .lessonId(lesson.getLessonId())
                .counterpartName(senderName)
                .courseName(courseName)
                .scheduledAt(lesson.getStartTime())
                .messageBody(messageBody)
                .senderUserId(senderId)
                .actionPath("/lesson/" + lesson.getLessonId())
                .isRead(false)
                .build());

        // Store a read copy for the sender so the inbox can render a full thread
        // without needing a separate message table.
        notificationDao.create(NotificationEntity.builder()
                .userId(senderId)
                .eventType(EVENT_LESSON_MESSAGE)
                .requestId(lesson.getRequestId())
                .lessonId(lesson.getLessonId())
                .counterpartName(recipientName)
                .courseName(courseName)
                .scheduledAt(lesson.getStartTime())
                .messageBody(messageBody)
                .senderUserId(senderId)
                .actionPath("/lesson/" + lesson.getLessonId())
                .isRead(true)
                .build());

        return recipientNotificationId;
    }

    public Long createAdminContactMessageNotification(
            Long contactId,
            Integer requesterId,
            Integer senderId,
            String subject,
            String messageBody,
            List<Integer> adminIds) {
        Set<Integer> adminIdSet = adminIds == null
                ? Set.of()
                : adminIds.stream()
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<Integer> participantIds = new LinkedHashSet<>();
        participantIds.add(requesterId);
        participantIds.addAll(adminIdSet);

        String requesterName = resolveUserDisplayName(requesterId);
        String normalizedSubject = subject == null ? "" : subject.trim();
        String actionPath = "/messages?contact=" + contactId;
        Long primaryNotificationId = null;

        for (Integer recipientId : participantIds) {
            boolean recipientIsAdmin = adminIdSet.contains(recipientId);
            boolean isSender = recipientId.equals(senderId);
            String counterpartName = recipientIsAdmin ? requesterName : MANAGER_TEAM_LABEL;

            Long notificationId = notificationDao.create(NotificationEntity.builder()
                    .userId(recipientId)
                    .eventType(EVENT_ADMIN_CONTACT_MESSAGE)
                    .contactId(contactId)
                    .counterpartName(counterpartName)
                    .courseName(normalizedSubject)
                    .messageBody(messageBody)
                    .senderUserId(senderId)
                    .actionPath(actionPath)
                    .isRead(isSender)
                    .build());

            if (!isSender && primaryNotificationId == null) {
                primaryNotificationId = notificationId;
            } else if (primaryNotificationId == null) {
                primaryNotificationId = notificationId;
            }
        }

        return primaryNotificationId;
    }

    public List<Map<String, Object>> adminContactThreadForUser(Integer userId, Long contactId) {
        return notificationDao.findByUser(userId, 200, 0, false, null, EVENT_ADMIN_CONTACT_MESSAGE, contactId).stream()
                .map(notification -> toPayload(notification, userId))
                .toList();
    }

    public List<Map<String, Object>> lessonMessageThreadForUser(Integer userId, Integer lessonId, int limit, int offset) {
        return notificationDao.findByUser(userId, limit, offset, false, lessonId, EVENT_LESSON_MESSAGE, null).stream()
                .map(notification -> toPayload(notification, userId))
                .toList();
    }

    public List<Map<String, Object>> unreadForUser(Integer userId, int limit) {
        return notificationDao.findUnreadByUser(userId, limit).stream()
                .map(notification -> toPayload(notification, userId))
                .toList();
    }

    public List<Map<String, Object>> listForUser(
            Integer userId,
            int limit,
            int offset,
            boolean unreadOnly,
            Integer lessonId,
            String eventType) {
        return notificationDao.findByUser(userId, limit, offset, unreadOnly, lessonId, normalizeEventType(eventType), null).stream()
                .map(notification -> toPayload(notification, userId))
                .toList();
    }

    public Map<String, Object> unreadCount(Integer userId) {
        return Map.of("count", notificationDao.countUnread(userId));
    }

    public Map<String, Object> markRead(Integer userId, List<Long> ids) {
        int updatedCount = (ids == null || ids.isEmpty())
                ? notificationDao.markAllRead(userId)
                : notificationDao.markRead(userId, ids);
        return Map.of("updatedCount", updatedCount);
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void createUpcomingLessonReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromTime = now.plusMinutes(50);
        LocalDateTime toTime = now.plusMinutes(70);

        lessonDao.findScheduledStartingBetween(fromTime, toTime)
                .forEach(this::createReminderForLessonIfNeeded);
    }

    private void createReminderForLessonIfNeeded(LessonEntity lesson) {
        createReminderForUserIfNeeded(lesson, lesson.getStudentId(), lesson.getTutorId());
        createReminderForUserIfNeeded(lesson, lesson.getTutorId(), lesson.getStudentId());
    }

    private void createReminderForUserIfNeeded(LessonEntity lesson, Integer userId, Integer counterpartId) {
        if (notificationDao.existsByUserEventAndLesson(userId, EVENT_LESSON_REMINDER, lesson.getLessonId())) {
            return;
        }

        notificationDao.create(NotificationEntity.builder()
                .userId(userId)
                .eventType(EVENT_LESSON_REMINDER)
                .requestId(lesson.getRequestId())
                .lessonId(lesson.getLessonId())
                .counterpartName(resolveUserDisplayName(counterpartId))
                .courseName(resolveCourseLabel(lesson.getCourseId()))
                .scheduledAt(lesson.getStartTime())
                .actionPath("/lesson/" + lesson.getLessonId())
                .isRead(false)
                .build());
    }

    private Map<String, Object> toPayload(NotificationEntity notification, Integer userId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", notification.getNotificationId());
        out.put("eventType", notification.getEventType());
        out.put("requestId", notification.getRequestId());
        out.put("lessonId", notification.getLessonId());
        out.put("contactId", notification.getContactId());
        out.put("counterpartName", notification.getCounterpartName());
        out.put("courseName", notification.getCourseName());
        out.put("subject", notification.getContactId() == null ? null : notification.getCourseName());
        out.put("scheduledAt", notification.getScheduledAt());
        out.put("rejectionReason", notification.getRejectionReason());
        out.put("messageBody", notification.getMessageBody());
        out.put("senderUserId", notification.getSenderUserId());
        out.put("senderName", resolveUserDisplayName(notification.getSenderUserId()));
        out.put("isOwnMessage", notification.getSenderUserId() != null && notification.getSenderUserId().equals(userId));
        out.put("actionPath", notification.getActionPath());
        out.put("isRead", notification.getIsRead());
        out.put("createdAt", notification.getCreatedAt());
        return out;
    }

    private String resolveUserDisplayName(Integer userId) {
        if (userId == null) {
            return "";
        }
        return userDao.findById(userId)
                .map(this::formatDisplayName)
                .orElse("");
    }

    private String resolveCourseLabel(Integer courseId) {
        if (courseId == null) {
            return "";
        }
        return courseDao.findById(courseId)
                .map(CourseLabelUtil::buildLabel)
                .orElse("");
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return null;
        }
        return eventType.trim().toUpperCase();
    }

    private String formatDisplayName(UserEntity user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getEmail() : fullName;
    }
}
