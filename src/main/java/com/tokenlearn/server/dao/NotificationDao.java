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
            .actionPath(rs.getString("action_path"))
            .isRead(rs.getBoolean("is_read"))
            .createdAt(rs.getObject("created_at", LocalDateTime.class))
            .readAt(rs.getObject("read_at", LocalDateTime.class))
            .build();

    public Long create(NotificationEntity notification) {
        String sql = """
                INSERT INTO notifications (
                  user_id, event_type, request_id, lesson_id, counterpart_name,
                  course_name, scheduled_at, rejection_reason, action_path, is_read
                )
                VALUES (
                  :userId, :eventType, :requestId, :lessonId, :counterpartName,
                  :courseName, :scheduledAt, :rejectionReason, :actionPath, :isRead
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
                .addValue("actionPath", notification.getActionPath())
                .addValue("isRead", Boolean.TRUE.equals(notification.getIsRead()));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[] { "notification_id" });
        return keyHolder.getKey().longValue();
    }

    public List<NotificationEntity> findUnreadByUser(Integer userId, int limit) {
        String sql = """
                SELECT notification_id, user_id, event_type, request_id, lesson_id, counterpart_name,
                       course_name, scheduled_at, rejection_reason, action_path, is_read, created_at, read_at
                FROM notifications
                WHERE user_id = :userId
                  AND is_read = 0
                ORDER BY created_at DESC
                OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", Math.max(1, limit)), mapper);
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
