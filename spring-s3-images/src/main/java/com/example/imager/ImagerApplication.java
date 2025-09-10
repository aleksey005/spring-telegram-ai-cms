package com.example.imager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // включаем автосканирование @ConfigurationProperties
public class ImagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImagerApplication.class, args);
    }
}
