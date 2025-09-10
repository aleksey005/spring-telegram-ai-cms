package com.example.tgpublisher.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private String botToken;
    private String chatId;

    public String getBotToken() { return botToken; }
    public void setBotToken(String botToken) { this.botToken = botToken; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    @PostConstruct
    public void validate() {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("TELEGRAM_BOT_TOKEN is not set");
        }
        if (chatId == null || chatId.isBlank()) {
            throw new IllegalStateException("TELEGRAM_CHAT_ID is not set");
        }
    }
}