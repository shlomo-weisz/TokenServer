package com.tokenlearn.server.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Stores short-lived password reset tokens and consumes them atomically when a reset is completed.
 */
@Repository
public class PasswordResetDao {
    private final NamedParameterJdbcTemplate jdbc;

    public PasswordResetDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createToken(String email, String token, LocalDateTime expiresAt) {
        String sql = "INSERT INTO password_reset_tokens(email, reset_token, expires_at, used) VALUES(:email,:token,:expiresAt,0)";
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("email", email)
                .addValue("token", token)
                .addValue("expiresAt", expiresAt));
    }

    public Optional<String> consumeIfValid(String email, String token) {
        String select = """
                SELECT email FROM password_reset_tokens
                WHERE email=:email AND reset_token=:token AND used=0 AND expires_at > GETUTCDATE()
                """;
        try {
            String found = jdbc.queryForObject(select,
                    new MapSqlParameterSource().addValue("email", email).addValue("token", token), String.class);
            String update = "UPDATE password_reset_tokens SET used=1 WHERE email=:email AND reset_token=:token";
            jdbc.update(update, new MapSqlParameterSource().addValue("email", email).addValue("token", token));
            return Optional.ofNullable(found);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
