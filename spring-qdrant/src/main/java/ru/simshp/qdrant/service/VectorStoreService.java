// /src/main/java/ru/simshp/qdrant/service/VectorStoreService.java
package ru.simshp.qdrant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.simshp.qdrant.api.dto.SearchHit;
import ru.simshp.qdrant.api.dto.SearchResponse;
import ru.simshp.qdrant.embedding.EmbeddingService;
import ru.simshp.qdrant.qdrant.QdrantClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final QdrantClient qdrant;
    private final EmbeddingService embeddingService;
    private final ObjectMapper mapper = new ObjectMapper();

    public String ingest(JsonNode json) {
        // Эмбеддим весь исходный JSON целиком (как строку).
        String asText = jsonToCompactString(json);
        float[] vec = embeddingService.embed(asText);

        String id = UUID.nameUUIDFromBytes(asText.getBytes(StandardCharsets.UTF_8)).toString();
        qdrant.upsertJson(id, vec, json);
        return id;
    }

    public SearchResponse search(String query, int topK) {
        float[] vec = embeddingService.embed(query);
        var results = qdrant.search(vec, topK);
        List<SearchHit> hits = results.stream()
                .map(r -> new SearchHit(r.score(), r.payload(), r.id()))
                .toList();
        return new SearchResponse(hits);
    }

    private String jsonToCompactString(JsonNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
