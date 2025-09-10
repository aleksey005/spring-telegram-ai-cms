package com.example.imageapi.service;

import com.example.imageapi.dto.ImageRequest;
import com.example.imageapi.dto.ImageResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OpenAIImageService {
  private final ObjectMapper mapper = new ObjectMapper();
  private final OkHttpClient http = new OkHttpClient.Builder()
      .callTimeout(Duration.ofMinutes(2))
      .connectTimeout(Duration.ofSeconds(20))
      .readTimeout(Duration.ofMinutes(2))
      .build();

  @Value("${openai.baseUrl}")
  private String baseUrl;

  @Value("${openai.apiKey}")
  private String apiKey;

  @Value("${openai.organization:}")
  private String org;

  public ImageResponse generate(ImageRequest req) throws IOException {
    Map<String, Object> payload = new LinkedHashMap<>();
    String model = (req.getModel() == null || req.getModel().isBlank()) ? "dall-e-3" : req.getModel();
payload.put("model", model);
    payload.put("prompt", req.getPrompt());
    payload.put("n", req.getN());
    payload.put("size", req.getSize());
    // Для gpt-image-1 параметр response_format не поддерживается; для остальных (например, dall-e-3) — поддерживается.
    if (!"gpt-image-1".equals(model)) {
      payload.put("response_format", req.isB64() ? "b64_json" : "url");
    }

    String json = mapper.writeValueAsString(payload);

    RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
    Request.Builder rb = new Request.Builder()
        .url(baseUrl + "/images/generations")
        .post(body)
        .addHeader("Authorization", "Bearer " + apiKey)
        .addHeader("Content-Type", "application/json");
    if (org != null && !org.isBlank()) {
      rb.addHeader("OpenAI-Organization", org);
    }

    try (Response resp = http.newCall(rb.build()).execute()) {
      if (!resp.isSuccessful()) {
        String errBody = resp.body() != null ? resp.body().string() : "";
        throw new IOException("OpenAI error: HTTP " + resp.code() + " - " + errBody);
      }
      String respBody = Objects.requireNonNull(resp.body()).string();
      OpenAiImagesResponse parsed = mapper.readValue(respBody, OpenAiImagesResponse.class);

      List<String> out = new ArrayList<>();
boolean b64 = req.isB64();
for (OpenAiImagesResponse.Data d : parsed.data) {
  if (b64) {
    // Ожидаем base64 либо напрямую (dall-e-3 с response_format=b64_json), либо у gpt-image-1
    if (d.b64Json != null && !d.b64Json.isBlank()) out.add(d.b64Json);
  } else {
    // Когда просили URL — используем URL, если вернулся; иначе эмулируем data-URL.
    if (d.url != null && !d.url.isBlank()) out.add(d.url);
    else if (d.b64Json != null && !d.b64Json.isBlank()) out.add("data:image/png;base64," + d.b64Json);
  }
}
return new ImageResponse(b64 ? "b64" : "url", out);
    }
  }

  // Вспомогательные классы для парсинга ответа OpenAI
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OpenAiImagesResponse {
    public List<Data> data;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
      @JsonProperty("b64_json") public String b64Json;
      public String url;
    }
  }
}