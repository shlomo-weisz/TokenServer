package com.tokenlearn.server.dao;

import com.tokenlearn.server.domain.CourseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Aggregation and maintenance queries used by the admin dashboard and contact flows.
 */
@Repository
public class AdminDao {
    private final NamedParameterJdbcTemplate jdbc;

    public AdminDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int totalUsers() {
        Integer value = jdbc.queryForObject("SELECT COUNT(*) FROM users", new MapSqlParameterSource(), Integer.class);
        return value == null ? 0 : value;
    }

    public int totalLessons() {
        Integer value = jdbc.queryForObject("SELECT COUNT(*) FROM lessons", new MapSqlParameterSource(), Integer.class);
        return value == null ? 0 : value;
    }

    public int totalRequests() {
        Integer value = jdbc.queryForObject("SELECT COUNT(*) FROM lesson_requests", new MapSqlParameterSource(), Integer.class);
        return value == null ? 0 : value;
    }

    public int pendingRequests() {
        Integer value = jdbc.queryForObject("SELECT COUNT(*) FROM lesson_requests WHERE status='PENDING'",
                new MapSqlParameterSource(), Integer.class);
        return value == null ? 0 : value;
    }

    public int lessonsThisMonth() {
        Integer value = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lessons WHERE created_at >= DATEFROMPARTS(YEAR(GETUTCDATE()), MONTH(GETUTCDATE()), 1)",
                new MapSqlParameterSource(), Integer.class);
        return value == null ? 0 : value;
    }

    public int lessonsThisWeek() {
        Integer value = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lessons WHERE created_at >= DATEADD(DAY, -7, GETUTCDATE())",
                new MapSqlParameterSource(), Integer.class);
        return value == null ? 0 : value;
    }

    public List<CourseEntity> mostPopularCourses(int limit) {
        String sql = """
                SELECT c.course_id, c.course_number, c.name_he, c.name_en, c.name, c.category, c.is_active, COUNT(*) AS lesson_count
                FROM lessons l
                INNER JOIN courses c ON c.course_id = l.course_id
                GROUP BY c.course_id, c.course_number, c.name_he, c.name_en, c.name, c.category, c.is_active
                ORDER BY lesson_count DESC
                OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.query(sql, new MapSqlParameterSource("limit", limit), (rs, rowNum) -> CourseEntity.builder()
                .courseId(rs.getInt("course_id"))
                .courseNumber(rs.getString("course_number"))
                .nameHe(rs.getString("name_he"))
                .nameEn(rs.getString("name_en"))
                .name(rs.getString("name"))
                .category(rs.getString("category"))
                .isActive(rs.getBoolean("is_active"))
                .build());
    }

    public Long createContact(Integer userId, String subject, String message) {
        String sql = "INSERT INTO admin_contacts(user_id, subject, message, status) VALUES(:userId,:subject,:message,'SUBMITTED')";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("subject", subject)
                .addValue("message", message), kh, new String[] { "contact_id" });
        return kh.getKey().longValue();
    }

    public List<Map<String, Object>> recentActivity(int limit) {
        String sql = """
                SELECT
                    'lesson_completed' AS type,
                    CAST(l.lesson_id AS VARCHAR(50)) AS item_id,
                    l.updated_at AS event_time
                FROM lessons l
                WHERE l.status = 'COMPLETED'
                ORDER BY l.updated_at DESC
                OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("limit", limit));
    }
}
