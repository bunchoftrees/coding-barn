package com.codingbarn.firehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FirehousePollingApplication {
    public static void main(String[] args) {
        SpringApplication.run(FirehousePollingApplication.class, args);
    }
}
