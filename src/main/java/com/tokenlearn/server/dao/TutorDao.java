package com.tokenlearn.server.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public class TutorDao {
    private final NamedParameterJdbcTemplate jdbc;

    public TutorDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findRecommended(int limit, BigDecimal minRating) {
        String sql = """
                SELECT
                    u.user_id AS id,
                    CONCAT(u.first_name, ' ', u.last_name) AS name,
                    u.photo_url AS photoUrl,
                    COALESCE(AVG(r.score), 0) AS rating
                FROM users u
                INNER JOIN user_courses_teacher uct ON uct.user_id = u.user_id
                LEFT JOIN ratings r ON r.to_user_id = u.user_id
                WHERE u.is_blocked_tutor = 0
                GROUP BY u.user_id, u.first_name, u.last_name, u.photo_url
                HAVING COALESCE(AVG(r.score), 0) >= :minRating
                ORDER BY rating DESC, name ASC
                OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("minRating", minRating));
    }

    public List<Map<String, Object>> searchTutors(String courseName, BigDecimal minRating, int limit) {
        String sql = """
                SELECT
                    u.user_id AS id,
                    CONCAT(u.first_name, ' ', u.last_name) AS name,
                    u.photo_url AS photoUrl,
                    COALESCE(AVG(r.score), 0) AS rating
                FROM users u
                INNER JOIN user_courses_teacher uct ON uct.user_id = u.user_id
                INNER JOIN courses c ON c.course_id = uct.course_id
                LEFT JOIN ratings r ON r.to_user_id = u.user_id
                WHERE u.is_blocked_tutor = 0
                  AND (:courseName IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :courseName, '%')))
                GROUP BY u.user_id, u.first_name, u.last_name, u.photo_url
                HAVING COALESCE(AVG(r.score), 0) >= :minRating
                ORDER BY rating DESC, name ASC
                OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource()
                .addValue("courseName", courseName)
                .addValue("minRating", minRating)
                .addValue("limit", limit));
    }

    public BigDecimal ratingForTutor(Integer tutorId) {
        String sql = "SELECT COALESCE(AVG(score),0) FROM ratings WHERE to_user_id=:id";
        BigDecimal value = jdbc.queryForObject(sql, new MapSqlParameterSource("id", tutorId), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }
}
