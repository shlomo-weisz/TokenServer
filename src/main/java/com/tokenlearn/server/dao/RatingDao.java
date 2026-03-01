package com.tokenlearn.server.dao;

import com.tokenlearn.server.domain.RatingEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class RatingDao {
    private final NamedParameterJdbcTemplate jdbc;

    public RatingDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<RatingEntity> mapper = (rs, rowNum) -> RatingEntity.builder()
            .ratingId(rs.getInt("rating_id"))
            .lessonId(rs.getInt("lesson_id"))
            .fromUserId(rs.getInt("from_user_id"))
            .toUserId(rs.getInt("to_user_id"))
            .score(rs.getBigDecimal("score"))
            .comment(rs.getString("comment"))
            .createdAt(rs.getObject("created_at", LocalDateTime.class))
            .build();

    public Integer create(RatingEntity rating) {
        String sql = """
                INSERT INTO ratings(lesson_id, from_user_id, to_user_id, score, comment)
                VALUES(:lessonId,:fromUserId,:toUserId,:score,:comment)
                """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("lessonId", rating.getLessonId())
                .addValue("fromUserId", rating.getFromUserId())
                .addValue("toUserId", rating.getToUserId())
                .addValue("score", rating.getScore())
                .addValue("comment", rating.getComment()), kh, new String[] { "rating_id" });
        return kh.getKey().intValue();
    }

    public List<RatingEntity> findForUser(Integer userId) {
        String sql = "SELECT * FROM ratings WHERE to_user_id=:userId ORDER BY created_at DESC";
        return jdbc.query(sql, new MapSqlParameterSource("userId", userId), mapper);
    }

    public BigDecimal averageForUser(Integer userId) {
        String sql = "SELECT COALESCE(AVG(score),0) FROM ratings WHERE to_user_id=:userId";
        BigDecimal value = jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    public int countForUser(Integer userId) {
        String sql = "SELECT COUNT(*) FROM ratings WHERE to_user_id=:userId";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), Integer.class);
        return count == null ? 0 : count;
    }

    public BigDecimal globalAverage() {
        String sql = "SELECT COALESCE(AVG(score),0) FROM ratings";
        BigDecimal value = jdbc.queryForObject(sql, new MapSqlParameterSource(), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }
}
