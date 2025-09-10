package com.example.telegramclient.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Публикация текста на эндпоинт /ingest в формате JSON.
 */
@Component
public class ExternalIngestPublisher {

    private static final Logger log = LoggerFactory.getLogger(ExternalIngestPublisher.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient http;
    private final boolean enabled;
    private final String url;
    private final ObjectMapper mapper = new ObjectMapper();

    public ExternalIngestPublisher(
            @Value("${app.publish.enabled:false}") boolean enabled,
            @Value("${app.publish.ingest-url:}") String url,
            @Value("${app.publish.timeout-ms:5000}") int timeoutMs
    ) {
        this.enabled = enabled;
        this.url = url;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Отправляет текст как POST application/json, если включено и задан URL.
     */
    public void ingestText(String text, long messageId) {
        if (!enabled) {
            log.debug("External publish disabled (app.publish.enabled=false)");
            return;
        }
        if (url == null || url.isBlank()) {
            log.warn("External publish enabled, но app.publish.ingest-url пуст");
            return;
        }
        Map<String, String> bodyMap = Map.of(
                "title", Long.toString(messageId),
                "text", text
        );
        String json;
        try {
            json = mapper.writeValueAsString(bodyMap);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации JSON: {}", e.toString(), e);
            return;
        }
        RequestBody body = RequestBody.create(json, JSON);
        Request req = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                log.error("Ingest HTTP {}: {}", resp.code(), respBody);
            } else {
                log.info("Опубликовано на ingest точку: HTTP {}", resp.code());
            }
        } catch (IOException e) {
            log.error("Ошибка ingestText: {}", e.toString(), e);
        }
    }
}

