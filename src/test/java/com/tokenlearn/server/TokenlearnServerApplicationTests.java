package com.tokenlearn.server;

import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class TokenlearnServerApplicationTests {

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private JmsTemplate jmsTemplate;

    @Test
    void contextLoads() {
    }
}
