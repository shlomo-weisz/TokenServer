package com.tokenlearn.server.dao;

import com.tokenlearn.server.domain.CourseEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Course catalog queries plus persistence helpers for teacher and student course associations.
 */
@Repository
public class CourseDao {
    private final NamedParameterJdbcTemplate jdbc;

    public CourseDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<CourseEntity> mapper = (rs, rowNum) -> CourseEntity.builder()
            .courseId(rs.getInt("course_id"))
            .courseNumber(rs.getString("course_number"))
            .nameHe(rs.getString("name_he"))
            .nameEn(rs.getString("name_en"))
            .name(rs.getString("name"))
            .category(rs.getString("category"))
            .isActive(rs.getBoolean("is_active"))
            .build();

    public List<CourseEntity> findAll(String search, String category, int limit) {
        String sql = """
                SELECT * FROM courses
                WHERE (
                    :search IS NULL
                    OR LOWER(name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(name_he) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(name_en) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR course_number LIKE CONCAT('%', :search, '%')
                )
                  AND (:category IS NULL OR LOWER(category) = LOWER(:category))
                ORDER BY
                    CASE WHEN course_number IS NULL THEN 1 ELSE 0 END,
                    course_number,
                    name_en,
                    name_he,
                    name
                OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("search", normalized(search))
                        .addValue("category", normalized(category))
                        .addValue("limit", Math.max(1, limit)),
                mapper);
    }

    public List<String> findCategories() {
        String sql = "SELECT DISTINCT category FROM courses WHERE category IS NOT NULL ORDER BY category";
        return jdbc.query(sql, (rs, i) -> rs.getString("category"));
    }

    public Optional<CourseEntity> findById(Integer id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT * FROM courses WHERE course_id=:id",
                    new MapSqlParameterSource("id", id),
                    mapper));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<CourseEntity> findByCourseNumber(String courseNumber) {
        String number = normalized(courseNumber);
        if (number == null) {
            return Optional.empty();
        }
        return findFirst(
                "SELECT TOP 1 * FROM courses WHERE course_number = :courseNumber",
                new MapSqlParameterSource("courseNumber", number));
    }

    public Optional<CourseEntity> findByIdentifier(String value) {
        String term = normalized(value);
        if (term == null) {
            return Optional.empty();
        }
        Optional<CourseEntity> byNumber = findByCourseNumber(term);
        if (byNumber.isPresent()) {
            return byNumber;
        }
        String possibleNumber = extractLeadingNumber(term);
        if (possibleNumber != null) {
            byNumber = findByCourseNumber(possibleNumber);
            if (byNumber.isPresent()) {
                return byNumber;
            }
        }
        String sql = """
                SELECT TOP 1 * FROM courses
                WHERE course_number = :term
                   OR LOWER(name_he) = LOWER(:term)
                   OR LOWER(name_en) = LOWER(:term)
                   OR LOWER(name) = LOWER(:term)
                ORDER BY course_id
                """;
        return findFirst(sql, new MapSqlParameterSource("term", term));
    }

    public void upsertCatalogCourse(String courseNumber, String nameHe, String nameEn, String category) {
        String number = truncate(normalized(courseNumber), 20);
        String he = truncate(normalized(nameHe), 255);
        String en = truncate(normalized(nameEn), 255);
        String normalizedCategory = normalized(category);
        if (number == null || (he == null && en == null)) {
            return;
        }

        String displayName = truncate(en != null ? en : he, 255);

        String attachNumberSql = """
                UPDATE courses
                SET course_number = :courseNumber,
                    name_he = COALESCE(NULLIF(:nameHe, ''), name_he),
                    name_en = COALESCE(NULLIF(:nameEn, ''), name_en),
                    name = COALESCE(NULLIF(:name, ''), name),
                    updated_at = GETUTCDATE()
                WHERE course_number IS NULL
                  AND (
                        LOWER(name_he) = LOWER(:nameHe)
                     OR LOWER(name_en) = LOWER(:nameEn)
                     OR LOWER(name) = LOWER(:nameHe)
                     OR LOWER(name) = LOWER(:nameEn)
                  )
                """;
        jdbc.update(attachNumberSql, new MapSqlParameterSource()
                .addValue("courseNumber", number)
                .addValue("nameHe", he == null ? "" : he)
                .addValue("nameEn", en == null ? "" : en)
                .addValue("name", displayName == null ? "" : displayName));

        String sql = """
                MERGE courses AS target
                USING (
                    SELECT :courseNumber AS course_number,
                           :nameHe AS name_he,
                           :nameEn AS name_en,
                           :name AS name,
                           :category AS category
                ) AS src
                ON target.course_number = src.course_number
                WHEN MATCHED THEN
                    UPDATE SET
                        target.name_he = src.name_he,
                        target.name_en = src.name_en,
                        target.name = src.name,
                        target.category = COALESCE(src.category, target.category),
                        target.is_active = 1,
                        target.updated_at = GETUTCDATE()
                WHEN NOT MATCHED THEN
                    INSERT (course_number, name_he, name_en, name, category, is_active)
                    VALUES (src.course_number, src.name_he, src.name_en, src.name, src.category, 1);
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("courseNumber", number)
                .addValue("nameHe", he == null ? "" : he)
                .addValue("nameEn", en == null ? "" : en)
                .addValue("name", displayName == null ? "" : displayName)
                .addValue("category", normalizedCategory));
    }

    public List<CourseEntity> findTeacherCourses(Integer userId) {
        String sql = """
                SELECT c.* FROM courses c
                INNER JOIN user_courses_teacher uct ON uct.course_id = c.course_id
                WHERE uct.user_id = :userId
                ORDER BY c.course_number, c.name_en, c.name_he, c.name
                """;
        return jdbc.query(sql, new MapSqlParameterSource("userId", userId), mapper);
    }

    public List<CourseEntity> findStudentCourses(Integer userId) {
        String sql = """
                SELECT c.* FROM courses c
                INNER JOIN user_courses_student ucs ON ucs.course_id = c.course_id
                WHERE ucs.user_id = :userId
                ORDER BY c.course_number, c.name_en, c.name_he, c.name
                """;
        return jdbc.query(sql, new MapSqlParameterSource("userId", userId), mapper);
    }

    public void replaceTeacherCourses(Integer userId, List<Integer> courseIds) {
        jdbc.update("DELETE FROM user_courses_teacher WHERE user_id=:userId", new MapSqlParameterSource("userId", userId));
        for (Integer courseId : courseIds) {
            jdbc.update("INSERT INTO user_courses_teacher(user_id, course_id) VALUES(:userId,:courseId)",
                    new MapSqlParameterSource().addValue("userId", userId).addValue("courseId", courseId));
        }
    }

    public void replaceStudentCourses(Integer userId, List<Integer> courseIds) {
        jdbc.update("DELETE FROM user_courses_student WHERE user_id=:userId", new MapSqlParameterSource("userId", userId));
        for (Integer courseId : courseIds) {
            jdbc.update("INSERT INTO user_courses_student(user_id, course_id) VALUES(:userId,:courseId)",
                    new MapSqlParameterSource().addValue("userId", userId).addValue("courseId", courseId));
        }
    }

    private Optional<CourseEntity> findFirst(String sql, MapSqlParameterSource params) {
        List<CourseEntity> rows = jdbc.query(sql, params, mapper);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String extractLeadingNumber(String value) {
        if (value == null) {
            return null;
        }
        int end = 0;
        while (end < value.length() && Character.isDigit(value.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return null;
        }
        return value.substring(0, end);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
