package org.example.producer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;
import java.util.random.RandomGenerator;

@Configuration
public class AppConfig {
    @Bean
    public RandomGenerator randomGenerator() {
        return new Random();
    }
}
