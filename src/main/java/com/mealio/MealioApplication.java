package com.mealio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry(proxyTargetClass = true)
public class MealioApplication {
    public static void main(String[] args) {
        SpringApplication.run(MealioApplication.class, args);
    }
}
