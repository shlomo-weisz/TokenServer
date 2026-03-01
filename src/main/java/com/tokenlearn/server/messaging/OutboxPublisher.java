package com.tokenlearn.server.messaging;

import com.tokenlearn.server.dao.OutboxDao;
import com.tokenlearn.server.domain.OutboxEventEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class OutboxPublisher {
    private static final String SETTLEMENT_QUEUE = "settlement-queue";

    private final OutboxDao outboxDao;
    private final JmsTemplate jmsTemplate;

    public OutboxPublisher(OutboxDao outboxDao, JmsTemplate jmsTemplate) {
        this.outboxDao = outboxDao;
        this.jmsTemplate = jmsTemplate;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void publishNewEvents() {
        List<OutboxEventEntity> events = outboxDao.findNewBatch();
        for (OutboxEventEntity event : events) {
            try {
                if ("LESSON_COMPLETED".equals(event.getEventType())) {
                    jmsTemplate.convertAndSend(SETTLEMENT_QUEUE, event.getPayloadJson());
                }
                outboxDao.markSent(event.getOutboxId());
                log.info("Outbox sent: id={}, messageId={}", event.getOutboxId(), event.getMessageId());
            } catch (Exception ex) {
                log.error("Failed to publish outbox id={}", event.getOutboxId(), ex);
                outboxDao.markFailed(event.getOutboxId(), ex.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void retryFailed() {
        List<OutboxEventEntity> failed = outboxDao.findFailedBatch();
        for (OutboxEventEntity e : failed) {
            outboxDao.resetFailed(e.getOutboxId());
        }
    }
}
