package com.tare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TareApplication {
    public static void main(String[] args) {
        SpringApplication.run(TareApplication.class, args);
    }
}
