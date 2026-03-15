package com.tokenlearn.server.dao;

import com.tokenlearn.server.domain.TokenTransactionEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persists token ledger entries and exposes audit queries for balances, settlements, and history views.
 */
@Repository
public class TokenTransactionDao {
    private final NamedParameterJdbcTemplate jdbc;

    public TokenTransactionDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<TokenTransactionEntity> mapper = (rs, rowNum) -> TokenTransactionEntity.builder()
            .txId(rs.getLong("tx_id"))
            .requestId((Integer) rs.getObject("request_id"))
            .lessonId((Integer) rs.getObject("lesson_id"))
            .payerId(rs.getInt("payer_id"))
            .receiverId(rs.getInt("receiver_id"))
            .amount(rs.getBigDecimal("amount"))
            .txType(rs.getString("tx_type"))
            .status(rs.getString("status"))
            .messageId(rs.getString("message_id"))
            .description(rs.getString("description"))
            .createdAt(rs.getObject("created_at", LocalDateTime.class))
            .build();

    public Long create(TokenTransactionEntity tx) {
        String sql = """
                INSERT INTO token_transactions(
                  request_id, lesson_id, payer_id, receiver_id, amount, tx_type, status, message_id, description
                )
                VALUES(
                  :requestId, :lessonId, :payerId, :receiverId, :amount, :txType, :status, :messageId, :description
                )
                """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", tx.getRequestId())
                .addValue("lessonId", tx.getLessonId())
                .addValue("payerId", tx.getPayerId())
                .addValue("receiverId", tx.getReceiverId())
                .addValue("amount", tx.getAmount())
                .addValue("txType", tx.getTxType())
                .addValue("status", tx.getStatus() == null ? "SUCCESS" : tx.getStatus())
                .addValue("messageId", tx.getMessageId())
                .addValue("description", tx.getDescription()), kh, new String[] { "tx_id" });
        return kh.getKey().longValue();
    }

    public boolean settlementExists(Integer requestId) {
        String sql = "SELECT COUNT(*) FROM token_transactions WHERE request_id=:id AND tx_type='SETTLEMENT' AND status='SUCCESS'";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("id", requestId), Integer.class);
        return count != null && count > 0;
    }

    public boolean refundExists(Integer requestId) {
        String sql = "SELECT COUNT(*) FROM token_transactions WHERE request_id=:id AND tx_type='REFUND' AND status='SUCCESS'";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("id", requestId), Integer.class);
        return count != null && count > 0;
    }

    public List<TokenTransactionEntity> findByUser(Integer userId, int limit, int offset) {
        String sql = """
                SELECT * FROM token_transactions
                WHERE payer_id = :userId OR receiver_id = :userId
                ORDER BY created_at DESC
                OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", limit)
                .addValue("offset", offset), mapper);
    }

    public int countByUser(Integer userId) {
        String sql = "SELECT COUNT(*) FROM token_transactions WHERE payer_id=:userId OR receiver_id=:userId";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), Integer.class);
        return count == null ? 0 : count;
    }
}
