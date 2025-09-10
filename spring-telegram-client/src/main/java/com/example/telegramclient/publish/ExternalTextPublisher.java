package com.example.telegramclient.publish;

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
import java.util.concurrent.TimeUnit;

@Component
public class ExternalTextPublisher {

    private static final Logger log = LoggerFactory.getLogger(ExternalTextPublisher.class);
    private static final MediaType TEXT = MediaType.parse("text/plain; charset=utf-8");

    private final OkHttpClient http;
    private final boolean enabled;
    private final String url;

    public ExternalTextPublisher(
            @Value("${app.publish.enabled:false}") boolean enabled,
            @Value("${app.publish.text-url:}") String url,
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

    /** Отправляет текст как POST text/plain, если включено и задан URL. */
    public void publishText(String text) {
        if (!enabled) {
            log.debug("External publish disabled (app.publish.enabled=false)");
            return;
        }
        if (url == null || url.isBlank()) {
            log.warn("External publish enabled, но app.publish.text-url пуст");
            return;
        }
        RequestBody body = RequestBody.create(text, TEXT);
        Request req = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "text/plain; charset=utf-8")
                .post(body)
                .build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                log.error("Publish HTTP {}: {}", resp.code(), respBody);
            } else {
                log.info("Опубликовано на внешнюю точку: HTTP {}", resp.code());
            }
        } catch (IOException e) {
            log.error("Ошибка publishText: {}", e.toString(), e);
        }
    }
}
