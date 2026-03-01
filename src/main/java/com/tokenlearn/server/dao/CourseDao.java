package com.tokenlearn.server.dao;

import com.tokenlearn.server.domain.CourseEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CourseDao {
    private final NamedParameterJdbcTemplate jdbc;

    public CourseDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<CourseEntity> mapper = (rs, rowNum) -> CourseEntity.builder()
            .courseId(rs.getInt("course_id"))
            .name(rs.getString("name"))
            .category(rs.getString("category"))
            .isActive(rs.getBoolean("is_active"))
            .build();

    public List<CourseEntity> findAll(String search, String category) {
        String sql = """
                SELECT * FROM courses
                WHERE (:search IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :search, '%')))
                  AND (:category IS NULL OR LOWER(category) = LOWER(:category))
                ORDER BY name
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("search", search).addValue("category", category), mapper);
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

    public Optional<CourseEntity> findByName(String name) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT TOP 1 * FROM courses WHERE LOWER(name)=LOWER(:name)",
                    new MapSqlParameterSource("name", name),
                    mapper));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Integer createIfMissing(String name, String category) {
        Optional<CourseEntity> existing = findByName(name);
        if (existing.isPresent()) {
            return existing.get().getCourseId();
        }
        String sql = "INSERT INTO courses(name, category, is_active) VALUES(:name,:category,1)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, new MapSqlParameterSource().addValue("name", name).addValue("category", category), kh,
                new String[] { "course_id" });
        return kh.getKey().intValue();
    }

    public List<CourseEntity> findTeacherCourses(Integer userId) {
        String sql = """
                SELECT c.* FROM courses c
                INNER JOIN user_courses_teacher uct ON uct.course_id = c.course_id
                WHERE uct.user_id = :userId
                ORDER BY c.name
                """;
        return jdbc.query(sql, new MapSqlParameterSource("userId", userId), mapper);
    }

    public List<CourseEntity> findStudentCourses(Integer userId) {
        String sql = """
                SELECT c.* FROM courses c
                INNER JOIN user_courses_student ucs ON ucs.course_id = c.course_id
                WHERE ucs.user_id = :userId
                ORDER BY c.name
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
}
