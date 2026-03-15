package com.tokenlearn.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenlearn.server.dao.LessonRequestDao;
import com.tokenlearn.server.dao.TokenTransactionDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.LessonRequestEntity;
import com.tokenlearn.server.domain.TokenTransactionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Finalizes token movement for completed lessons after the JMS round-trip.
 *
 * <p>Idempotency is handled by checking whether a settlement transaction already
 * exists for the lesson request before moving balances again.
 */
@Slf4j
@Service
public class SettlementService {
    private final UserDao userDao;
    private final LessonRequestDao lessonRequestDao;
    private final TokenTransactionDao tokenTransactionDao;
    private final ObjectMapper objectMapper;

    public SettlementService(UserDao userDao, LessonRequestDao lessonRequestDao, TokenTransactionDao tokenTransactionDao, ObjectMapper objectMapper) {
        this.userDao = userDao;
        this.lessonRequestDao = lessonRequestDao;
        this.tokenTransactionDao = tokenTransactionDao;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processSettlement(String payloadJson) {
        try {
            JsonNode payload = objectMapper.readTree(payloadJson);
            Integer requestId = payload.get("requestId").asInt();
            Integer lessonId = payload.get("lessonId").asInt();
            Integer studentId = payload.get("studentId").asInt();
            Integer tutorId = payload.get("tutorId").asInt();
            BigDecimal amount = payload.get("amount").decimalValue();
            String messageId = payload.get("messageId").asText();

            // Retries are expected, so settlement must be safe to execute more
            // than once for the same request.
            if (tokenTransactionDao.settlementExists(requestId)) {
                log.info("Settlement already exists for request {}", requestId);
                return;
            }

            LessonRequestEntity req = lessonRequestDao.findById(requestId).orElse(null);
            if (req == null) {
                throw new IllegalStateException("Missing lesson request " + requestId);
            }
            if (req.getTokenCost().compareTo(amount) != 0) {
                throw new IllegalStateException("Amount mismatch for request " + requestId);
            }

            boolean settled = userDao.settleLockedToTutor(studentId, tutorId, amount);
            if (!settled) {
                throw new IllegalStateException("Unable to settle locked tokens");
            }

            tokenTransactionDao.create(TokenTransactionEntity.builder()
                    .requestId(requestId)
                    .lessonId(lessonId)
                    .payerId(studentId)
                    .receiverId(tutorId)
                    .amount(amount)
                    .txType("SETTLEMENT")
                    .status("SUCCESS")
                    .messageId(messageId)
                    .description("Tokens transferred to tutor after lesson completion")
                    .build());
            log.info("Settlement completed for request {}, message {}", requestId, messageId);
        } catch (Exception ex) {
            log.error("Settlement failed for payload {}", payloadJson, ex);
            throw new RuntimeException(ex);
        }
    }
}
