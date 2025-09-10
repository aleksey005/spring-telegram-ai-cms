package com.example.tgpublisher.service;

import com.example.tgpublisher.config.TelegramProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TelegramClient {
    private final WebClient webClient;
    private final TelegramProperties props;

    public TelegramClient(TelegramProperties props) {
        this.props = props;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
    }

    public TelegramApiModels.SendPhotoResponse sendPhoto(byte[] imageBytes, String filename, String caption) {
        Assert.notNull(imageBytes, "imageBytes must not be null");
        if (caption != null && caption.length() > 1024) {
            throw new IllegalArgumentException("Caption must be <= 1024 characters");
        }
        String path = "/bot" + props.getBotToken() + "/sendPhoto";

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("chat_id", props.getChatId());
        if (caption != null && !caption.isBlank()) mb.part("caption", caption);
        mb.part("photo", new NamedByteArrayResource(imageBytes, filename))
          .contentType(MediaType.APPLICATION_OCTET_STREAM);

        return this.webClient.post()
                .uri(path)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(mb.build()))
                .retrieve()
                .bodyToMono(TelegramApiModels.SendPhotoResponse.class)
                .onErrorResume(ex -> Mono.error(new RuntimeException("Telegram API call failed: " + ex.getMessage(), ex)))
                .block();
    }

    public TelegramApiModels.SendMessageResponse sendMessage(String text) {
        Assert.notNull(text, "text must not be null");
        if (text.length() > 4096) {
            throw new IllegalArgumentException("Text must be <= 4096 characters");
        }
        String path = "/bot" + props.getBotToken() + "/sendMessage";
        return this.webClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of(
                        "chat_id", props.getChatId(),
                        "text", text
                ))
                .retrieve()
                .bodyToMono(TelegramApiModels.SendMessageResponse.class)
                .onErrorResume(ex -> Mono.error(new RuntimeException("Telegram API call failed: " + ex.getMessage(), ex)))
                .block();
    }

    static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;
        public NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = (filename == null || filename.isBlank()) ? "image" : filename;
        }
        @Override
        public String getFilename() { return filename; }
    }
}