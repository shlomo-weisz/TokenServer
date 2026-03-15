package com.tokenlearn.server.dao;

import com.tokenlearn.server.domain.AvailabilityEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

/**
 * Persists availability slots by user and role and replaces the slot set atomically when profiles are updated.
 */
@Repository
public class AvailabilityDao {
    private final NamedParameterJdbcTemplate jdbc;

    public AvailabilityDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<AvailabilityEntity> mapper = (rs, rowNum) -> AvailabilityEntity.builder()
            .availabilityId(rs.getInt("availability_id"))
            .userId(rs.getInt("user_id"))
            .role(rs.getString("role"))
            .day(rs.getString("day"))
            .startTime(rs.getObject("start_time", LocalTime.class))
            .endTime(rs.getObject("end_time", LocalTime.class))
            .build();

    public List<AvailabilityEntity> findByUserAndRole(Integer userId, String role) {
        String sql = "SELECT * FROM availability WHERE user_id=:userId AND role=:role ORDER BY day, start_time";
        return jdbc.query(sql, new MapSqlParameterSource().addValue("userId", userId).addValue("role", role), mapper);
    }

    public void replaceForUserRole(Integer userId, String role, List<AvailabilityEntity> slots) {
        jdbc.update("DELETE FROM availability WHERE user_id=:userId AND role=:role",
                new MapSqlParameterSource().addValue("userId", userId).addValue("role", role));
        for (AvailabilityEntity slot : slots) {
            String sql = """
                    INSERT INTO availability(user_id, role, day, start_time, end_time)
                    VALUES(:userId,:role,:day,:start,:end)
                    """;
            jdbc.update(sql, new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("role", role)
                    .addValue("day", slot.getDay())
                    .addValue("start", slot.getStartTime())
                    .addValue("end", slot.getEndTime()));
        }
    }
}
