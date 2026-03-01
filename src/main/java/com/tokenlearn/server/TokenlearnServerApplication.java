package com.tokenlearn.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TokenlearnServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TokenlearnServerApplication.class, args);
    }
}
