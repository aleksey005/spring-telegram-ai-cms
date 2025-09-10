package com.example.telegramclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.example.telegramclient.config")
public class TelegramClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelegramClientApplication.class, args);
    }
}
