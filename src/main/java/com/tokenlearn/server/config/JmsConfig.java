package com.tokenlearn.server.config;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;

import jakarta.jms.ConnectionFactory;

@Configuration
public class JmsConfig {
    @Value("${spring.artemis.broker-url}")
    private String brokerUrl;
    @Value("${spring.artemis.user}")
    private String user;
    @Value("${spring.artemis.password}")
    private String password;

    @Bean
    public ConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory delegate = new ActiveMQConnectionFactory(brokerUrl, user, password);
        CachingConnectionFactory caching = new CachingConnectionFactory(delegate);
        caching.setSessionCacheSize(10);
        return caching;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);
        factory.setErrorHandler(t -> {
        });
        return factory;
    }
}
