package com.example.tgpublisher.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramApiModels {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SendPhotoResponse {
        private boolean ok;
        private Message result;
        public boolean isOk() { return ok; }
        public void setOk(boolean ok) { this.ok = ok; }
        public Message getResult() { return result; }
        public void setResult(Message result) { this.result = result; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SendMessageResponse {
        private boolean ok;
        private Message result;
        public boolean isOk() { return ok; }
        public void setOk(boolean ok) { this.ok = ok; }
        public Message getResult() { return result; }
        public void setResult(Message result) { this.result = result; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        @JsonProperty("message_id")
        private Long messageId;
        public Long getMessageId() { return messageId; }
        public void setMessageId(Long messageId) { this.messageId = messageId; }
    }
}