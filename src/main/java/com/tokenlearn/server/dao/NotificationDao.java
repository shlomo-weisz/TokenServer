package com.tokenlearn.server.dao;

import com.tokenlearn.server.domain.NotificationEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class NotificationDao {
    private final NamedParameterJdbcTemplate jdbc;

    public NotificationDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<NotificationEntity> mapper = (rs, rowNum) -> NotificationEntity.builder()
            .notificationId(rs.getLong("notification_id"))
            .userId(rs.getInt("user_id"))
            .eventType(rs.getString("event_type"))
            .requestId((Integer) rs.getObject("request_id"))
            .lessonId((Integer) rs.getObject("lesson_id"))
            .counterpartName(rs.getString("counterpart_name"))
            .courseName(rs.getString("course_name"))
            .scheduledAt(rs.getObject("scheduled_at", LocalDateTime.class))
            .rejectionReason(rs.getString("rejection_reason"))
            .messageBody(rs.getString("message_body"))
            .senderUserId((Integer) rs.getObject("sender_user_id"))
            .actionPath(rs.getString("action_path"))
            .isRead(rs.getBoolean("is_read"))
            .createdAt(rs.getObject("created_at", LocalDateTime.class))
            .readAt(rs.getObject("read_at", LocalDateTime.class))
            .build();

    public Long create(NotificationEntity notification) {
        String sql = """
                INSERT INTO notifications (
                  user_id, event_type, request_id, lesson_id, counterpart_name,
                  course_name, scheduled_at, rejection_reason, message_body, sender_user_id,
                  action_path, is_read
                )
                VALUES (
                  :userId, :eventType, :requestId, :lessonId, :counterpartName,
                  :courseName, :scheduledAt, :rejectionReason, :messageBody, :senderUserId,
                  :actionPath, :isRead
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", notification.getUserId())
                .addValue("eventType", notification.getEventType())
                .addValue("requestId", notification.getRequestId())
                .addValue("lessonId", notification.getLessonId())
                .addValue("counterpartName", notification.getCounterpartName())
                .addValue("courseName", notification.getCourseName())
                .addValue("scheduledAt", notification.getScheduledAt())
                .addValue("rejectionReason", notification.getRejectionReason())
                .addValue("messageBody", notification.getMessageBody())
                .addValue("senderUserId", notification.getSenderUserId())
                .addValue("actionPath", notification.getActionPath())
                .addValue("isRead", Boolean.TRUE.equals(notification.getIsRead()));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[] { "notification_id" });
        return keyHolder.getKey().longValue();
    }

    public List<NotificationEntity> findUnreadByUser(Integer userId, int limit) {
        return findByUser(userId, limit, 0, true, null, null);
    }

    public List<NotificationEntity> findByUser(
            Integer userId,
            int limit,
            int offset,
            boolean unreadOnly,
            Integer lessonId,
            String eventType) {
        String sql = """
                SELECT notification_id, user_id, event_type, request_id, lesson_id, counterpart_name,
                       course_name, scheduled_at, rejection_reason, message_body, sender_user_id,
                       action_path, is_read, created_at, read_at
                FROM notifications
                WHERE user_id = :userId
                  AND (:unreadOnly = 0 OR is_read = 0)
                  AND (:lessonId IS NULL OR lesson_id = :lessonId)
                  AND (:eventType IS NULL OR event_type = :eventType)
                ORDER BY created_at DESC
                OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("unreadOnly", unreadOnly ? 1 : 0)
                .addValue("lessonId", lessonId)
                .addValue("eventType", eventType)
                .addValue("offset", Math.max(0, offset))
                .addValue("limit", Math.max(1, limit)), mapper);
    }

    public int countUnread(Integer userId) {
        String sql = """
                SELECT COUNT(*)
                FROM notifications
                WHERE user_id = :userId
                  AND is_read = 0
                """;
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), Integer.class);
        return count == null ? 0 : count;
    }

    public boolean existsByUserEventAndLesson(Integer userId, String eventType, Integer lessonId) {
        String sql = """
                SELECT COUNT(*)
                FROM notifications
                WHERE user_id = :userId
                  AND event_type = :eventType
                  AND lesson_id = :lessonId
                """;
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("eventType", eventType)
                .addValue("lessonId", lessonId), Integer.class);
        return count != null && count > 0;
    }

    public int markRead(Integer userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        String sql = """
                UPDATE notifications
                SET is_read = 1,
                    read_at = GETUTCDATE()
                WHERE user_id = :userId
                  AND is_read = 0
                  AND notification_id IN (:ids)
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("ids", ids));
    }

    public int markAllRead(Integer userId) {
        String sql = """
                UPDATE notifications
                SET is_read = 1,
                    read_at = GETUTCDATE()
                WHERE user_id = :userId
                  AND is_read = 0
                """;
        return jdbc.update(sql, new MapSqlParameterSource("userId", userId));
    }
}
