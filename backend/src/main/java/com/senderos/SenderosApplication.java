package com.senderos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SenderosApplication {

    public static void main(String[] args) {
        SpringApplication.run(SenderosApplication.class, args);
    }
}
