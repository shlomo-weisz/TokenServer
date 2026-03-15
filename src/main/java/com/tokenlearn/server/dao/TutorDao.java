package com.tokenlearn.server.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Search-oriented read model for tutor discovery, recommendation, and rating lookups.
 */
@Repository
public class TutorDao {
    private final NamedParameterJdbcTemplate jdbc;

    public TutorDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findRecommended(Integer excludeUserId, int limit, BigDecimal minRating) {
        String sql = """
                SELECT
                    u.user_id AS id,
                    CONCAT(u.first_name, ' ', u.last_name) AS name,
                    u.photo_url AS photoUrl,
                    u.about_me_as_teacher AS aboutMeAsTeacher,
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM lessons ls
                            WHERE ls.student_id = :excludeUserId
                              AND ls.tutor_id = u.user_id
                              AND ls.status = 'COMPLETED'
                        ) THEN CAST(1 AS BIT)
                        ELSE CAST(0 AS BIT)
                    END AS taughtMeBefore,
                    COALESCE(AVG(r.score), 0) AS rating,
                    COUNT(DISTINCT CASE WHEN l.status = 'COMPLETED' THEN l.lesson_id END) AS lessons
                FROM users u
                INNER JOIN user_courses_teacher uct ON uct.user_id = u.user_id
                LEFT JOIN ratings r ON r.to_user_id = u.user_id
                LEFT JOIN lessons l ON l.tutor_id = u.user_id
                WHERE u.is_blocked_tutor = 0
                  AND u.user_id <> :excludeUserId
                GROUP BY u.user_id, u.first_name, u.last_name, u.photo_url, u.about_me_as_teacher
                HAVING COALESCE(AVG(r.score), 0) >= :minRating
                ORDER BY rating DESC, name ASC
                OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource()
                .addValue("excludeUserId", excludeUserId)
                .addValue("limit", limit)
                .addValue("minRating", minRating));
    }

    public List<Map<String, Object>> searchTutors(
            Integer excludeUserId,
            String courseName,
            String tutorName,
            BigDecimal minRating,
            Boolean taughtMeBefore,
            int limit) {
        String sql = """
                SELECT
                    u.user_id AS id,
                    CONCAT(u.first_name, ' ', u.last_name) AS name,
                    u.photo_url AS photoUrl,
                    u.about_me_as_teacher AS aboutMeAsTeacher,
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM lessons ls
                            WHERE ls.student_id = :excludeUserId
                              AND ls.tutor_id = u.user_id
                              AND ls.status = 'COMPLETED'
                        ) THEN CAST(1 AS BIT)
                        ELSE CAST(0 AS BIT)
                    END AS taughtMeBefore,
                    COALESCE(AVG(r.score), 0) AS rating,
                    COUNT(DISTINCT CASE WHEN l.status = 'COMPLETED' THEN l.lesson_id END) AS lessons
                FROM users u
                INNER JOIN user_courses_teacher uct ON uct.user_id = u.user_id
                INNER JOIN courses c ON c.course_id = uct.course_id
                LEFT JOIN ratings r ON r.to_user_id = u.user_id
                LEFT JOIN lessons l ON l.tutor_id = u.user_id
                WHERE u.is_blocked_tutor = 0
                  AND u.user_id <> :excludeUserId
                  AND (
                      :tutorName IS NULL
                      OR LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :tutorName, '%'))
                      OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :tutorName, '%'))
                      OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :tutorName, '%'))
                  )
                  AND (
                      :courseName IS NULL
                      OR LOWER(c.name) LIKE LOWER(CONCAT('%', :courseName, '%'))
                      OR LOWER(c.name_he) LIKE LOWER(CONCAT('%', :courseName, '%'))
                      OR LOWER(c.name_en) LIKE LOWER(CONCAT('%', :courseName, '%'))
                      OR c.course_number LIKE CONCAT('%', :courseName, '%')
                  )
                  AND (
                      :taughtMeBefore IS NULL
                      OR :taughtMeBefore = 0
                      OR EXISTS (
                          SELECT 1
                          FROM lessons ls
                          WHERE ls.student_id = :excludeUserId
                            AND ls.tutor_id = u.user_id
                            AND ls.status = 'COMPLETED'
                      )
                  )
                GROUP BY u.user_id, u.first_name, u.last_name, u.photo_url, u.about_me_as_teacher
                HAVING COALESCE(AVG(r.score), 0) >= :minRating
                ORDER BY rating DESC, name ASC
                OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource()
                .addValue("excludeUserId", excludeUserId)
                .addValue("courseName", courseName)
                .addValue("tutorName", tutorName)
                .addValue("minRating", minRating)
                .addValue("taughtMeBefore", taughtMeBefore)
                .addValue("limit", limit));
    }

    public BigDecimal ratingForTutor(Integer tutorId) {
        String sql = "SELECT COALESCE(AVG(score),0) FROM ratings WHERE to_user_id=:id";
        BigDecimal value = jdbc.queryForObject(sql, new MapSqlParameterSource("id", tutorId), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }
}
