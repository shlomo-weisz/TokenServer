package com.tokenlearn.server.messaging;

import com.tokenlearn.server.service.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * JMS consumer for settlement events emitted by the transactional outbox.
 */
@Slf4j
@Component
public class SettlementConsumer {
    private final SettlementService settlementService;

    public SettlementConsumer(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @JmsListener(destination = "settlement-queue", containerFactory = "jmsListenerContainerFactory")
    public void consume(String payloadJson) {
        settlementService.processSettlement(payloadJson);
        log.info("Settlement message processed");
    }
}
