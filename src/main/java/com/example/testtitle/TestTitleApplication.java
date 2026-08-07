package com.example.testtitle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TestTitleApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestTitleApplication.class, args);
    }

}
