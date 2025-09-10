package com.example.tgpublisher.model;

public class PublishResponse {
    private boolean ok;
    private Long telegramMessageId;

    public PublishResponse(boolean ok, Long telegramMessageId) {
        this.ok = ok;
        this.telegramMessageId = telegramMessageId;
    }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public Long getTelegramMessageId() { return telegramMessageId; }
    public void setTelegramMessageId(Long telegramMessageId) { this.telegramMessageId = telegramMessageId; }
}