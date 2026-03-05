package com.tokenlearn.server.dao;

import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.dto.TokenBalancesDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDao {
    private final NamedParameterJdbcTemplate jdbc;

    public UserDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<UserEntity> mapper = (rs, rowNum) -> UserEntity.builder()
            .userId(rs.getInt("user_id"))
            .email(rs.getString("email"))
            .passwordHash(rs.getString("password_hash"))
            .firstName(rs.getString("first_name"))
            .lastName(rs.getString("last_name"))
            .phone(rs.getString("phone"))
            .photoUrl(rs.getString("photo_url"))
            .secretQuestion(rs.getString("secret_question"))
            .secretAnswerHash(rs.getString("secret_answer_hash"))
            .aboutMeAsTeacher(rs.getString("about_me_as_teacher"))
            .aboutMeAsStudent(rs.getString("about_me_as_student"))
            .isAdmin(rs.getBoolean("is_admin"))
            .isActive(rs.getBoolean("is_active"))
            .isBlockedTutor(rs.getBoolean("is_blocked_tutor"))
            .availableBalance(rs.getBigDecimal("available_balance"))
            .lockedBalance(rs.getBigDecimal("locked_balance"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public Optional<UserEntity> findById(Integer userId) {
        String sql = "SELECT * FROM users WHERE user_id = :userId";
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), mapper));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<UserEntity> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = :email";
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, new MapSqlParameterSource("email", email), mapper));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Integer create(UserEntity user) {
        String sql = """
                INSERT INTO users (
                  email, password_hash, first_name, last_name, phone,
                  secret_question, secret_answer_hash,
                  is_admin, is_active, available_balance, locked_balance
                )
                VALUES (
                  :email, :passwordHash, :firstName, :lastName, :phone,
                  :secretQuestion, :secretAnswerHash,
                  :isAdmin, :isActive, :availableBalance, :lockedBalance
                )
                """;
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("email", user.getEmail())
                .addValue("passwordHash", user.getPasswordHash())
                .addValue("firstName", user.getFirstName())
                .addValue("lastName", user.getLastName())
                .addValue("phone", user.getPhone())
                .addValue("secretQuestion", user.getSecretQuestion())
                .addValue("secretAnswerHash", user.getSecretAnswerHash())
                .addValue("isAdmin", Boolean.TRUE.equals(user.getIsAdmin()))
                .addValue("isActive", user.getIsActive() == null || user.getIsActive())
                .addValue("availableBalance", user.getAvailableBalance() == null ? BigDecimal.ZERO : user.getAvailableBalance())
                .addValue("lockedBalance", user.getLockedBalance() == null ? BigDecimal.ZERO : user.getLockedBalance());
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, p, kh, new String[] { "user_id" });
        return kh.getKey().intValue();
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = :email";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("email", email), Integer.class);
        return count != null && count > 0;
    }

    public boolean existsByEmailExcludingUser(String email, Integer userId) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = :email AND user_id <> :userId";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource()
                .addValue("email", email)
                .addValue("userId", userId), Integer.class);
        return count != null && count > 0;
    }

    public int countUsers() {
        String sql = "SELECT COUNT(*) FROM users";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
        return count == null ? 0 : count;
    }

    public void updatePasswordByEmail(String email, String passwordHash) {
        String sql = "UPDATE users SET password_hash=:passwordHash, updated_at=GETUTCDATE() WHERE email=:email";
        jdbc.update(sql, new MapSqlParameterSource().addValue("email", email).addValue("passwordHash", passwordHash));
    }

    public void updateProfile(Integer userId, String firstName, String lastName, String phone, String photoUrl,
            String aboutTeacher, String aboutStudent) {
        String sql = """
                UPDATE users
                SET first_name = COALESCE(:firstName, first_name),
                    last_name = COALESCE(:lastName, last_name),
                    phone = COALESCE(:phone, phone),
                    photo_url = COALESCE(:photoUrl, photo_url),
                    about_me_as_teacher = COALESCE(:aboutTeacher, about_me_as_teacher),
                    about_me_as_student = COALESCE(:aboutStudent, about_me_as_student),
                    updated_at = GETUTCDATE()
                WHERE user_id = :userId
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("firstName", firstName)
                .addValue("lastName", lastName)
                .addValue("phone", phone)
                .addValue("photoUrl", photoUrl)
                .addValue("aboutTeacher", aboutTeacher)
                .addValue("aboutStudent", aboutStudent));
    }

    public void updatePhoto(Integer userId, String photoUrl) {
        String sql = "UPDATE users SET photo_url=:photoUrl, updated_at=GETUTCDATE() WHERE user_id=:userId";
        jdbc.update(sql, new MapSqlParameterSource().addValue("userId", userId).addValue("photoUrl", photoUrl));
    }

    public void updateByAdmin(
            Integer userId,
            String email,
            String firstName,
            String lastName,
            String phone,
            String photoUrl,
            String aboutTeacher,
            String aboutStudent,
            boolean isAdmin,
            boolean isBlockedTutor,
            boolean isActive) {
        String sql = """
                UPDATE users
                SET email = :email,
                    first_name = :firstName,
                    last_name = :lastName,
                    phone = :phone,
                    photo_url = :photoUrl,
                    about_me_as_teacher = :aboutTeacher,
                    about_me_as_student = :aboutStudent,
                    is_admin = :isAdmin,
                    is_blocked_tutor = :isBlockedTutor,
                    is_active = :isActive,
                    updated_at = GETUTCDATE()
                WHERE user_id = :userId
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("email", email)
                .addValue("firstName", firstName)
                .addValue("lastName", lastName)
                .addValue("phone", phone)
                .addValue("photoUrl", photoUrl)
                .addValue("aboutTeacher", aboutTeacher)
                .addValue("aboutStudent", aboutStudent)
                .addValue("isAdmin", isAdmin)
                .addValue("isBlockedTutor", isBlockedTutor)
                .addValue("isActive", isActive));
    }

    public boolean reserveTokens(Integer userId, BigDecimal amount) {
        String sql = """
                UPDATE u
                SET available_balance = available_balance - :amount,
                    locked_balance = locked_balance + :amount,
                    updated_at = GETUTCDATE()
                FROM users u WITH (UPDLOCK, ROWLOCK)
                WHERE user_id = :userId AND available_balance >= :amount
                """;
        return jdbc.update(sql, new MapSqlParameterSource().addValue("userId", userId).addValue("amount", amount)) == 1;
    }

    public boolean refundTokens(Integer userId, BigDecimal amount) {
        String sql = """
                UPDATE u
                SET locked_balance = locked_balance - :amount,
                    available_balance = available_balance + :amount,
                    updated_at = GETUTCDATE()
                FROM users u WITH (UPDLOCK, ROWLOCK)
                WHERE user_id = :userId AND locked_balance >= :amount
                """;
        return jdbc.update(sql, new MapSqlParameterSource().addValue("userId", userId).addValue("amount", amount)) == 1;
    }

    public boolean settleLockedToTutor(Integer studentId, Integer tutorId, BigDecimal amount) {
        String debit = """
                UPDATE u
                SET locked_balance = locked_balance - :amount, updated_at = GETUTCDATE()
                FROM users u WITH (UPDLOCK, ROWLOCK)
                WHERE user_id = :studentId AND locked_balance >= :amount
                """;
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("tutorId", tutorId)
                .addValue("amount", amount);
        if (jdbc.update(debit, p) != 1) {
            return false;
        }
        String credit = """
                UPDATE u
                SET available_balance = available_balance + :amount, updated_at = GETUTCDATE()
                FROM users u WITH (UPDLOCK, ROWLOCK)
                WHERE user_id = :tutorId
                """;
        return jdbc.update(credit, p) == 1;
    }

    public boolean transferAvailable(Integer fromUserId, Integer toUserId, BigDecimal amount) {
        String debit = """
                UPDATE u
                SET available_balance = available_balance - :amount, updated_at = GETUTCDATE()
                FROM users u WITH (UPDLOCK, ROWLOCK)
                WHERE user_id = :fromUserId AND available_balance >= :amount
                """;
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("fromUserId", fromUserId)
                .addValue("toUserId", toUserId)
                .addValue("amount", amount);
        if (jdbc.update(debit, p) != 1) {
            return false;
        }
        String credit = """
                UPDATE u
                SET available_balance = available_balance + :amount, updated_at = GETUTCDATE()
                FROM users u WITH (UPDLOCK, ROWLOCK)
                WHERE user_id = :toUserId
                """;
        return jdbc.update(credit, p) == 1;
    }

    public void addAvailable(Integer userId, BigDecimal amount) {
        String sql = """
                UPDATE users
                SET available_balance = available_balance + :amount,
                    updated_at = GETUTCDATE()
                WHERE user_id = :userId
                """;
        jdbc.update(sql, new MapSqlParameterSource().addValue("userId", userId).addValue("amount", amount));
    }

    public boolean subtractAvailable(Integer userId, BigDecimal amount) {
        String sql = """
                UPDATE u
                SET available_balance = available_balance - :amount,
                    updated_at = GETUTCDATE()
                FROM users u WITH (UPDLOCK, ROWLOCK)
                WHERE user_id = :userId AND available_balance >= :amount
                """;
        return jdbc.update(sql, new MapSqlParameterSource().addValue("userId", userId).addValue("amount", amount)) == 1;
    }

    public TokenBalancesDto getBalances(Integer userId) {
        String sql = """
                SELECT
                    u.available_balance AS available,
                    u.locked_balance AS locked,
                    (u.available_balance + u.locked_balance) AS total,
                    COALESCE((
                        SELECT SUM(lr.token_cost)
                        FROM lesson_requests lr
                        WHERE lr.tutor_id = :userId AND lr.status IN ('APPROVED')
                    ), 0) AS futureTutorEarnings
                FROM users u
                WHERE u.user_id = :userId
                """;
        return jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), (rs, rowNum) -> TokenBalancesDto.builder()
                .available(rs.getBigDecimal("available"))
                .locked(rs.getBigDecimal("locked"))
                .total(rs.getBigDecimal("total"))
                .futureTutorEarnings(rs.getBigDecimal("futureTutorEarnings"))
                .pendingTransfers(rs.getBigDecimal("locked"))
                .build());
    }

    public void setTutorBlocked(Integer tutorId, boolean blocked) {
        String sql = "UPDATE users SET is_blocked_tutor=:blocked, updated_at=GETUTCDATE() WHERE user_id=:userId";
        jdbc.update(sql, new MapSqlParameterSource().addValue("blocked", blocked).addValue("userId", tutorId));
    }

    public List<UserEntity> listUsers(int limit, int offset) {
        String sql = """
                SELECT * FROM users
                ORDER BY user_id
                OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("offset", offset).addValue("limit", limit), mapper);
    }

    public int hardDeleteUser(Integer userId, String email) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("email", email);

        String userRequestIdsSql = "SELECT request_id FROM lesson_requests WHERE student_id = :userId OR tutor_id = :userId";
        String userLessonIdsSql = "SELECT lesson_id FROM lessons WHERE student_id = :userId OR tutor_id = :userId";

        jdbc.update(
                "DELETE FROM token_transactions WHERE payer_id = :userId OR receiver_id = :userId OR request_id IN (" + userRequestIdsSql
                        + ") OR lesson_id IN (" + userLessonIdsSql + ")",
                params);
        jdbc.update(
                "DELETE FROM ratings WHERE from_user_id = :userId OR to_user_id = :userId OR lesson_id IN (" + userLessonIdsSql + ")",
                params);
        jdbc.update("DELETE FROM lessons WHERE lesson_id IN (" + userLessonIdsSql + ")", params);
        jdbc.update("DELETE FROM lesson_requests WHERE request_id IN (" + userRequestIdsSql + ")", params);
        jdbc.update("DELETE FROM admin_contacts WHERE user_id = :userId", params);
        jdbc.update("DELETE FROM password_reset_tokens WHERE email = :email", params);
        return jdbc.update("DELETE FROM users WHERE user_id = :userId", params);
    }
}
