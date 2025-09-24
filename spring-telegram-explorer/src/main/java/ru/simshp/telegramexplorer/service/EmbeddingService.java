package ru.simshp.telegramexplorer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.simshp.telegramexplorer.config.ExplorerProperties;
import ru.simshp.telegramexplorer.domain.MessageEntity;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ExplorerProperties props;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void upsertEmbedding(MessageEntity message, String jsonPayload) {
        var text = (message.getText() != null && !message.getText().isBlank())
                ? message.getText()
                : (message.getCaption() != null ? message.getCaption() : "");

        if (text.isBlank()) return;

        var client = new OpenAiService(props.getOpenai().getApiKey(), Duration.ofSeconds(60));
        var request = EmbeddingRequest.builder()
                .model(props.getOpenai().getEmbeddingsModel())
                .input(List.of(text))
                .build();

        var response = client.createEmbeddings(request);
        var vector = response.getData().get(0).getEmbedding();

        // pgvector: формируем литерал массива float8[] -> vector
        // Простейший способ — использовать формат '[x,y,...]'::vector
        String vectorLiteral = "[" + String.join(",", vector.stream().map(Object::toString).toList()) + "]";

        // upsert в таблицу embedding
        jdbcTemplate.update("""
            INSERT INTO embedding(message_id, json_payload, vector)
            VALUES (?, CAST(? AS jsonb), CAST(? AS vector))
            ON CONFLICT (message_id)
            DO UPDATE SET json_payload = EXCLUDED.json_payload, vector = EXCLUDED.vector
        """,
                message.getId(),
                jsonPayload,
                vectorLiteral
        );
    }

    /** Утилита для JSON-документа */
    public String buildJsonDocument(MessageEntity msg, List<Map<String, Object>> media) {
        var id = UUID.randomUUID().toString();
        var sb = new StringBuilder();
        sb.append("{")
          .append("\"id\":\"").append(id).append("\",")
          .append("\"chatId\":").append(msg.getTgChatId()).append(",")
          .append("\"messageId\":").append(msg.getTgMessageId()).append(",")
          .append("\"threadId\":").append(msg.getThreadId() == null ? "null" : msg.getThreadId()).append(",")
            .append("\"channelUsername\":").append(toJsonStringOrNull(msg.getChannel() == null ? null : msg.getChannel().getUsername())).append(",")
            .append("\"authorUsername\":").append(toJsonStringOrNull(msg.getAuthorUsername())).append(",")
          .append("\"type\":\"").append(msg.isComment() ? "comment" : "channel_post").append("\",")
          .append("\"textOrCaption\":").append(toJsonStringOrNull(msg.getText() != null ? msg.getText() : msg.getCaption())).append(",")
          .append("\"media\":[");
        for (int i=0; i<media.size(); i++) {
            var m = media.get(i);
            if (i>0) sb.append(",");
            sb.append("{")
              .append("\"kind\":").append(toJsonStringOrNull((String) m.getOrDefault("kind", "other"))).append(",")
              .append("\"mimeType\":").append(toJsonStringOrNull((String)m.get("mimeType"))).append(",")
              .append("\"path\":").append(toJsonStringOrNull((String)m.get("path")))
              .append("}");
        }
        sb.append("],")
          .append("\"timestamp\":").append(toJsonStringOrNull(msg.getPublishedAt() != null ? msg.getPublishedAt().toString() : null))
          .append("}");
        return sb.toString();
    }

    private String toJsonStringOrNull(String s) {
        if (s == null) return "null";
        try {
            return objectMapper.writeValueAsString(s);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize string to JSON", e);
        }
    }
}
