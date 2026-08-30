package com.example.shadowvibe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShadowVibeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShadowVibeApplication.class, args);
    }

}
