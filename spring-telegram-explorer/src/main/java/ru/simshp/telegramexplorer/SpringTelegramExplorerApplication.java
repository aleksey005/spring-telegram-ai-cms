package ru.simshp.telegramexplorer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringTelegramExplorerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringTelegramExplorerApplication.class, args);
    }
}
