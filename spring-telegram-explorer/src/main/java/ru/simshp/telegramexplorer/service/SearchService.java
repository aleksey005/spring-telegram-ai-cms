package ru.simshp.telegramexplorer.service;

import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.simshp.telegramexplorer.config.ExplorerProperties;
import ru.simshp.telegramexplorer.web.dto.MessageView;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private static final int DEFAULT_LIMIT = 20;
    private static final Duration OPENAI_TIMEOUT = Duration.ofSeconds(60);

    private final ExplorerProperties props;
    private final JdbcTemplate jdbcTemplate;

    public List<MessageView> search(String query) {
        return search(query, DEFAULT_LIMIT);
    }

    public List<MessageView> search(String query, int limit) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }

        int effectiveLimit = limit <= 0 ? DEFAULT_LIMIT : limit;

        try {
            List<MessageView> vectorResults = searchByEmbeddings(trimmed, effectiveLimit);
            if (!vectorResults.isEmpty()) {
                return vectorResults;
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to search using embeddings: {}", ex.getMessage());
        }

        return searchByPlainText(trimmed, effectiveLimit);
    }

    private List<MessageView> searchByEmbeddings(String query, int limit) {
        String vectorLiteral = createEmbeddingLiteral(query);

        return jdbcTemplate.query("""
                        WITH query_embedding AS (
                            SELECT CAST(? AS vector) AS embedding
                        )
                        SELECT m.id,
                               c.username AS channel_username,
                               m.is_comment,
                               m.thread_id,
                               m.author_username,
                               m.text,
                               m.caption,
                               m.has_media,
                               m.published_at
                        FROM embedding e
                        JOIN query_embedding q ON TRUE
                        JOIN message m ON m.id = e.message_id
                        LEFT JOIN channel c ON c.id = m.channel_id
                        ORDER BY e.vector <=> q.embedding
                        LIMIT ?
                        """,
                ps -> {
                    ps.setString(1, vectorLiteral);
                    ps.setInt(2, limit);
                },
                this::mapRow
        );
    }

    private List<MessageView> searchByPlainText(String query, int limit) {
        String pattern = toLikePattern(query);

        return jdbcTemplate.query("""
                        SELECT m.id,
                               c.username AS channel_username,
                               m.is_comment,
                               m.thread_id,
                               m.author_username,
                               m.text,
                               m.caption,
                               m.has_media,
                               m.published_at
                        FROM message m
                        LEFT JOIN channel c ON c.id = m.channel_id
                        WHERE (m.text ILIKE ? ESCAPE '\\')
                           OR (m.caption ILIKE ? ESCAPE '\\')
                        ORDER BY m.published_at DESC NULLS LAST, m.id DESC
                        LIMIT ?
                        """,
                ps -> {
                    ps.setString(1, pattern);
                    ps.setString(2, pattern);
                    ps.setInt(3, limit);
                },
                this::mapRow
        );
    }

    protected String createEmbeddingLiteral(String text) {
        var client = new OpenAiService(props.getOpenai().getApiKey(), OPENAI_TIMEOUT);
        var request = EmbeddingRequest.builder()
                .model(props.getOpenai().getEmbeddingsModel())
                .input(List.of(text))
                .build();

        var response = client.createEmbeddings(request);
        var vector = response.getData().get(0).getEmbedding();
        return "[" + String.join(",", vector.stream().map(Object::toString).toList()) + "]";
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private MessageView mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MessageView(
                rs.getLong("id"),
                defaultString(rs.getString("channel_username")),
                rs.getBoolean("is_comment"),
                rs.getObject("thread_id", Long.class),
                rs.getString("author_username"),
                rs.getString("text"),
                rs.getString("caption"),
                rs.getBoolean("has_media"),
                rs.getObject("published_at", OffsetDateTime.class)
        );
    }

    private String toLikePattern(String query) {
        String escaped = query
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
