package com.example.tgpublisher.controller;

import com.example.tgpublisher.model.PublishResponse;
import com.example.tgpublisher.service.TelegramApiModels;
import com.example.tgpublisher.service.TelegramClient;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
public class PublishController {

    private final TelegramClient telegramClient;

    public PublishController(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @PostMapping(value = "/publish", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PublishResponse publish(
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "caption", required = false)
            @Size(max = 2048, message = "Caption must be <= 2048 characters") String caption
    ) throws Exception {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        byte[] bytes = image.getBytes();
        String filename = image.getOriginalFilename();
        TelegramApiModels.SendPhotoResponse resp = telegramClient.sendPhoto(bytes, filename, caption);
        if (resp == null || !resp.isOk()) {
            throw new RuntimeException("Telegram did not accept the message");
        }
        Long messageId = (resp.getResult() != null) ? resp.getResult().getMessageId() : null;
        return new PublishResponse(true, messageId);
    }

    @PostMapping(value = "/publish/text", consumes = MediaType.TEXT_PLAIN_VALUE)
    public PublishResponse publishText(
            @RequestBody @Size(max = 4096, message = "Text must be <= 4096 characters") String text
    ) {
        TelegramApiModels.SendMessageResponse resp = telegramClient.sendMessage(text);
        if (resp == null || !resp.isOk()) {
            throw new RuntimeException("Telegram did not accept the message");
        }
        Long messageId = (resp.getResult() != null) ? resp.getResult().getMessageId() : null;
        return new PublishResponse(true, messageId);
    }
}