package com.tokenlearn.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the TokenLearn backend.
 */
@SpringBootApplication
@EnableScheduling
public class TokenlearnServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TokenlearnServerApplication.class, args);
    }
}
