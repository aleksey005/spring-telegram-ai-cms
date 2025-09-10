package com.example.telegramclient.publish;

import dev.voroby.springframework.telegram.client.TelegramClient;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service that uploads Telegram photos to external HTTP endpoint.
 */
@Component
public class ImagePublisher {

    private static final Logger log = LoggerFactory.getLogger(ImagePublisher.class);
    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient http;
    private final boolean enabled;
    private final String uploadUrl;

    public ImagePublisher(
            @Value("${app.publish.enabled:false}") boolean enabled,
            @Value("${app.publish.image-url:https://www.you_site.ru/imager/upload}") String uploadUrl,
            @Value("${app.publish.timeout-ms:5000}") int timeoutMs
    ) {
        this.enabled = enabled;
        this.uploadUrl = uploadUrl;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    /** Download image and publish it as Base64 JSON. */
    public void publishPhoto(TelegramClient client, TdApi.MessagePhoto photo, long messageId) {
        if (!enabled) {
            log.debug("Image publish disabled (app.publish.enabled=false)");
            return;
        }
        if (uploadUrl == null || uploadUrl.isBlank()) {
            log.warn("Image publish enabled, но app.publish.image-url пуст");
            return;
        }
        try {
            String path = downloadFileAndGetPath(client, photo);
            if (path == null || path.isBlank()) {
                log.warn("Локальный путь не найден для messageId={}", messageId);
                return;
            }
            Path filePath = Path.of(path);
            byte[] data = Files.readAllBytes(filePath);
            String base64 = Base64.getEncoder().encodeToString(data);
            String contentType = Optional.ofNullable(Files.probeContentType(filePath))
                    .orElse("image/jpeg");
            String body = String.format(
                    "{\"message_id\":\"%s\",\"image_base64\":\"data:%s;base64,%s\"}",
                    messageId,
                    contentType,
                    base64
            );
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(RequestBody.create(body, JSON))
                    .build();
            try (Response resp = http.newCall(request).execute()) {
                log.info("Опубликовано изображение messageId={} : HTTP {}", messageId, resp.code());
            }
        } catch (IOException | InterruptedException e) {
            log.warn("Failed to publish photo", e);
        }
    }

    private String downloadFileAndGetPath(TelegramClient client, TdApi.MessagePhoto photo) throws InterruptedException {
        TdApi.PhotoSize[] sizes = photo.photo.sizes;
        if (sizes.length == 0) {
            return null;
        }
        int fileId = sizes[sizes.length - 1].photo.id;
        TdApi.DownloadFile d = new TdApi.DownloadFile(fileId, 32, 0L, 0L, true);
        client.send(d);
        for (int i = 0; i < 50; i++) {
            var fresp = client.send(new TdApi.GetFile(fileId));
            Optional<TdApi.File> fo = fresp.getObject();
            if (fo.isPresent()) {
                TdApi.File f = fo.get();
                if (f.local != null && f.local.isDownloadingCompleted) {
                    return f.local.path;
                }
            }
            Thread.sleep(200);
        }
        var last = client.send(new TdApi.GetFile(fileId)).getObject();
        return last.map(f -> f.local != null ? f.local.path : null).orElse(null);
    }
}

