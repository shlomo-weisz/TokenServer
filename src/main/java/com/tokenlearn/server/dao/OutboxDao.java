package com.tokenlearn.server.dao;

import com.tokenlearn.server.domain.OutboxEventEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persists and transitions transactional outbox events for asynchronous publication.
 */
@Repository
public class OutboxDao {
    private final NamedParameterJdbcTemplate jdbc;

    public OutboxDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<OutboxEventEntity> mapper = (rs, rowNum) -> OutboxEventEntity.builder()
            .outboxId(rs.getLong("outbox_id"))
            .aggregateType(rs.getString("aggregate_type"))
            .aggregateId(rs.getInt("aggregate_id"))
            .eventType(rs.getString("event_type"))
            .payloadJson(rs.getString("payload_json"))
            .messageId(rs.getString("message_id"))
            .status(rs.getString("status"))
            .createdAt(rs.getObject("created_at", LocalDateTime.class))
            .sentAt(rs.getObject("sent_at", LocalDateTime.class))
            .errorMessage(rs.getString("error_message"))
            .build();

    public Long create(OutboxEventEntity e) {
        String sql = """
                INSERT INTO jms_outbox(aggregate_type, aggregate_id, event_type, payload_json, message_id, status)
                VALUES(:aggregateType, :aggregateId, :eventType, :payloadJson, :messageId, :status)
                """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("aggregateType", e.getAggregateType())
                .addValue("aggregateId", e.getAggregateId())
                .addValue("eventType", e.getEventType())
                .addValue("payloadJson", e.getPayloadJson())
                .addValue("messageId", e.getMessageId())
                .addValue("status", e.getStatus()), kh, new String[] { "outbox_id" });
        return kh.getKey().longValue();
    }

    public List<OutboxEventEntity> findNewBatch() {
        String sql = "SELECT TOP 100 * FROM jms_outbox WHERE status='NEW' ORDER BY created_at";
        return jdbc.query(sql, mapper);
    }

    public List<OutboxEventEntity> findFailedBatch() {
        String sql = "SELECT TOP 100 * FROM jms_outbox WHERE status='FAILED' ORDER BY created_at";
        return jdbc.query(sql, mapper);
    }

    public void markSent(Long outboxId) {
        jdbc.update("UPDATE jms_outbox SET status='SENT', sent_at=GETUTCDATE() WHERE outbox_id=:id",
                new MapSqlParameterSource("id", outboxId));
    }

    public void markFailed(Long outboxId, String error) {
        jdbc.update("UPDATE jms_outbox SET status='FAILED', error_message=:error WHERE outbox_id=:id",
                new MapSqlParameterSource().addValue("id", outboxId).addValue("error", error));
    }

    public void resetFailed(Long outboxId) {
        jdbc.update("UPDATE jms_outbox SET status='NEW', error_message=NULL WHERE outbox_id=:id",
                new MapSqlParameterSource("id", outboxId));
    }
}
