package com.example.telegramclient.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;

@Component
public class OpenAiClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String apiBaseUrl;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final String systemPrompt;
    private final String userPrompt;

    public OpenAiClient(
            @Value("${app.openai.api-key}") String apiKey,
            @Value("${app.openai.api-base-url:https://api.openai.com/v1}") String apiBaseUrl,
            @Value("${app.openai.model:gpt-4o-mini}") String model,
            @Value("${app.openai.max-tokens:80}") int maxTokens,
            @Value("${app.openai.temperature:0.7}") double temperature,
            @Value("${app.openai.system-prompt:Ты — лаконичный комментатор изображений для телеграм-канала. Пиши 1–2 короткие строки по делу, без хэштегов, без эмодзи, без обращений и упоминаний.}") String systemPrompt,
            @Value("${app.openai.user-prompt:Сделай короткий комментарий к этому изображению для канала.}") String userPrompt
    ) {
        this.apiKey = apiKey;
        this.apiBaseUrl = apiBaseUrl;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
    }

    public String captionImage(byte[] imageBytes, String mimeType) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY / app.openai.api-key не задан");
        }
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64;

        // Собираем корректный JSON через Jackson
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", temperature);
        root.put("max_tokens", maxTokens);

        ArrayNode messages = root.putArray("messages");

        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);

        ObjectNode user = messages.addObject();
        user.put("role", "user");
        ArrayNode contentArr = user.putArray("content");

        ObjectNode textPart = contentArr.addObject();
        textPart.put("type", "text");
        textPart.put("text", userPrompt);

        ObjectNode imagePart = contentArr.addObject();
        imagePart.put("type", "image_url");
        ObjectNode imageUrl = imagePart.putObject("image_url");
        imageUrl.put("url", dataUrl);

        String payload = mapper.writeValueAsString(root);

        Request req = new Request.Builder()
                .url(apiBaseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(payload, JSON))
                .build();

        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                throw new IOException("OpenAI error: HTTP " + resp.code() + " " + body);
            }
            JsonNode rootResp = mapper.readTree(body);
            JsonNode contentNode = rootResp.path("choices").path(0).path("message").path("content");
            return contentNode.isTextual() ? contentNode.asText() : "[нет ответа]";
        }
    }
}
