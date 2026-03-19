package com.tokenlearn.server.dao;

import com.tokenlearn.server.domain.LessonEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Persists lessons and exposes query shapes for user calendars, history, reminders, and admin views.
 */
@Repository
public class LessonDao {
    private final NamedParameterJdbcTemplate jdbc;

    public LessonDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<LessonEntity> mapper = (rs, rowNum) -> LessonEntity.builder()
            .lessonId(rs.getInt("lesson_id"))
            .requestId(rs.getInt("request_id"))
            .studentId(rs.getInt("student_id"))
            .tutorId(rs.getInt("tutor_id"))
            .courseId(rs.getInt("course_id"))
            .tokenCost(rs.getBigDecimal("token_cost"))
            .startTime(rs.getObject("start_time", LocalDateTime.class))
            .endTime(rs.getObject("end_time", LocalDateTime.class))
            .status(rs.getString("status"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public Integer create(LessonEntity lesson) {
        String sql = """
                INSERT INTO lessons(request_id, student_id, tutor_id, course_id, token_cost, start_time, end_time, status)
                VALUES(:requestId,:studentId,:tutorId,:courseId,:tokenCost,:startTime,:endTime,:status)
                """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", lesson.getRequestId())
                .addValue("studentId", lesson.getStudentId())
                .addValue("tutorId", lesson.getTutorId())
                .addValue("courseId", lesson.getCourseId())
                .addValue("tokenCost", lesson.getTokenCost())
                .addValue("startTime", lesson.getStartTime())
                .addValue("endTime", lesson.getEndTime())
                .addValue("status", lesson.getStatus()), kh, new String[] { "lesson_id" });
        return kh.getKey().intValue();
    }

    public Optional<LessonEntity> findById(Integer lessonId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("SELECT * FROM lessons WHERE lesson_id=:id",
                    new MapSqlParameterSource("id", lessonId), mapper));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<LessonEntity> findByRequestId(Integer requestId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("SELECT * FROM lessons WHERE request_id=:id",
                    new MapSqlParameterSource("id", requestId), mapper));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public int updateStatus(Integer lessonId, String status) {
        String sql = "UPDATE lessons SET status=:status, updated_at=GETUTCDATE() WHERE lesson_id=:id";
        return jdbc.update(sql, new MapSqlParameterSource().addValue("id", lessonId).addValue("status", status));
    }

    public int transitionStatus(Integer lessonId, String expectedStatus, String newStatus) {
        String sql = """
                UPDATE lessons
                SET status=:newStatus, updated_at=GETUTCDATE()
                WHERE lesson_id=:id
                  AND status=:expectedStatus
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", lessonId)
                .addValue("expectedStatus", expectedStatus)
                .addValue("newStatus", newStatus));
    }

    public List<LessonEntity> findUpcomingByUser(Integer userId, String role) {
        String sql = """
                SELECT * FROM lessons
                WHERE status='SCHEDULED'
                  AND start_time >= GETUTCDATE()
                  AND (
                      (:role='teacher' AND tutor_id=:userId)
                      OR (:role='student' AND student_id=:userId)
                      OR (:role IS NULL AND (student_id=:userId OR tutor_id=:userId))
                  )
                ORDER BY start_time ASC
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("userId", userId).addValue("role", role), mapper);
    }

    public List<LessonEntity> findHistoryByUser(Integer userId, int limit, int offset) {
        String sql = """
                SELECT * FROM lessons
                WHERE (student_id=:userId OR tutor_id=:userId)
                  AND status IN ('COMPLETED','CANCELLED')
                ORDER BY start_time DESC
                OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", limit)
                .addValue("offset", offset), mapper);
    }

    public int countHistoryByUser(Integer userId) {
        String sql = """
                SELECT COUNT(*)
                FROM lessons
                WHERE (student_id=:userId OR tutor_id=:userId)
                  AND status IN ('COMPLETED','CANCELLED')
                """;
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), Integer.class);
        return count == null ? 0 : count;
    }

    public List<LessonEntity> findAllForAdmin(String status, int limit, int offset) {
        String sql = """
                SELECT * FROM lessons
                WHERE (:status IS NULL OR LOWER(status)=LOWER(:status))
                ORDER BY created_at DESC
                OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("limit", limit)
                .addValue("offset", offset), mapper);
    }

    public int countAllForAdmin(String status) {
        String sql = "SELECT COUNT(*) FROM lessons WHERE (:status IS NULL OR LOWER(status)=LOWER(:status))";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("status", status), Integer.class);
        return count == null ? 0 : count;
    }

    public List<LessonEntity> findScheduledStartingBetween(LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT * FROM lessons
                WHERE status = 'SCHEDULED'
                  AND start_time >= :fromTime
                  AND start_time <= :toTime
                ORDER BY start_time ASC
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("fromTime", from)
                .addValue("toTime", to), mapper);
    }

    public List<LessonEntity> findScheduledEndingBefore(LocalDateTime cutoff) {
        String sql = """
                SELECT * FROM lessons
                WHERE status = 'SCHEDULED'
                  AND end_time <= :cutoff
                ORDER BY end_time ASC
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("cutoff", cutoff), mapper);
    }

    public List<LessonEntity> findByUserBetween(Integer userId, String role, String status, LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT * FROM lessons
                WHERE start_time >= :fromTime
                  AND start_time < :toTime
                  AND (:status IS NULL OR UPPER(status) = UPPER(:status))
                  AND (
                      (:role='teacher' AND tutor_id=:userId)
                      OR (:role='student' AND student_id=:userId)
                      OR (:role IS NULL AND (student_id=:userId OR tutor_id=:userId))
                  )
                ORDER BY start_time ASC
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("role", role)
                .addValue("status", status)
                .addValue("fromTime", from)
                .addValue("toTime", to), mapper);
    }

}
