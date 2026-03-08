package com.tokenlearn.server.dao;

import com.tokenlearn.server.domain.LessonRequestEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public class LessonRequestDao {
    private final NamedParameterJdbcTemplate jdbc;

    public LessonRequestDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<LessonRequestEntity> mapper = (rs, rowNum) -> LessonRequestEntity.builder()
            .requestId(rs.getInt("request_id"))
            .studentId(rs.getInt("student_id"))
            .tutorId(rs.getInt("tutor_id"))
            .courseId(rs.getInt("course_id"))
            .tokenCost(rs.getBigDecimal("token_cost"))
            .requestedDay(rs.getString("requested_day"))
            .requestedStartTime(rs.getObject("requested_start_time", LocalTime.class))
            .requestedEndTime(rs.getObject("requested_end_time", LocalTime.class))
            .specificStartTime(rs.getObject("specific_start_time", LocalDateTime.class))
            .specificEndTime(rs.getObject("specific_end_time", LocalDateTime.class))
            .message(rs.getString("message"))
            .status(rs.getString("status"))
            .rejectionMessage(rs.getString("rejection_message"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public Optional<LessonRequestEntity> findById(Integer requestId) {
        String sql = "SELECT * FROM lesson_requests WHERE request_id=:id";
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, new MapSqlParameterSource("id", requestId), mapper));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Integer create(LessonRequestEntity req) {
        String sql = """
                INSERT INTO lesson_requests (
                  student_id, tutor_id, course_id, token_cost,
                  requested_day, requested_start_time, requested_end_time,
                  specific_start_time, specific_end_time, message, status
                )
                VALUES (
                  :studentId, :tutorId, :courseId, :tokenCost,
                  :requestedDay, :requestedStartTime, :requestedEndTime,
                  :specificStartTime, :specificEndTime, :message, :status
                )
                """;
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("studentId", req.getStudentId())
                .addValue("tutorId", req.getTutorId())
                .addValue("courseId", req.getCourseId())
                .addValue("tokenCost", req.getTokenCost())
                .addValue("requestedDay", req.getRequestedDay())
                .addValue("requestedStartTime", req.getRequestedStartTime())
                .addValue("requestedEndTime", req.getRequestedEndTime())
                .addValue("specificStartTime", req.getSpecificStartTime())
                .addValue("specificEndTime", req.getSpecificEndTime())
                .addValue("message", req.getMessage())
                .addValue("status", req.getStatus());
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, p, kh, new String[] { "request_id" });
        return kh.getKey().intValue();
    }

    public int updateStatus(Integer requestId, String status) {
        String sql = "UPDATE lesson_requests SET status=:status, updated_at=GETUTCDATE() WHERE request_id=:id";
        return jdbc.update(sql, new MapSqlParameterSource().addValue("id", requestId).addValue("status", status));
    }

    public int transitionStatus(Integer requestId, String expectedStatus, String newStatus) {
        String sql = """
                UPDATE lesson_requests
                SET status=:newStatus, updated_at=GETUTCDATE()
                WHERE request_id=:id
                  AND status=:expectedStatus
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", requestId)
                .addValue("expectedStatus", expectedStatus)
                .addValue("newStatus", newStatus));
    }

    public int transitionStatusWithRejection(Integer requestId, String expectedStatus, String newStatus, String reason) {
        String sql = """
                UPDATE lesson_requests
                SET status=:newStatus, rejection_message=:reason, updated_at=GETUTCDATE()
                WHERE request_id=:id
                  AND status=:expectedStatus
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", requestId)
                .addValue("expectedStatus", expectedStatus)
                .addValue("newStatus", newStatus)
                .addValue("reason", reason));
    }

    public int updateStatusWithRejection(Integer requestId, String status, String reason) {
        String sql = """
                UPDATE lesson_requests
                SET status=:status, rejection_message=:reason, updated_at=GETUTCDATE()
                WHERE request_id=:id
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", requestId)
                .addValue("status", status)
                .addValue("reason", reason));
    }

    public List<LessonRequestEntity> findByStudent(Integer userId, String status) {
        String sql = """
                SELECT * FROM lesson_requests
                WHERE student_id = :userId
                  AND (:status IS NULL OR UPPER(status) = UPPER(:status))
                ORDER BY created_at DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("userId", userId).addValue("status", status), mapper);
    }

    public List<LessonRequestEntity> findByTutor(Integer userId, String status) {
        String sql = """
                SELECT * FROM lesson_requests
                WHERE tutor_id = :userId
                  AND (:status IS NULL OR UPPER(status) = UPPER(:status))
                ORDER BY created_at DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("userId", userId).addValue("status", status), mapper);
    }

    public List<LessonRequestEntity> findPendingExpiringBefore(LocalDateTime latestAllowedStartTime) {
        String sql = """
                SELECT * FROM lesson_requests
                WHERE status = 'PENDING'
                  AND specific_start_time IS NOT NULL
                  AND specific_start_time <= :latestAllowedStartTime
                ORDER BY specific_start_time ASC, request_id ASC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("latestAllowedStartTime", latestAllowedStartTime), mapper);
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM lesson_requests WHERE status = :status";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("status", status), Integer.class);
        return count == null ? 0 : count;
    }
}
