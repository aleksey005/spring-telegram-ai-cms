// /src/main/java/ru/simshp/qdrant/embedding/OpenAiEmbeddingService.java
package ru.simshp.qdrant.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.simshp.qdrant.config.AppProperties;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAiEmbeddingService implements EmbeddingService {

    private final WebClient openaiClient;
    private final AppProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiEmbeddingService(@Qualifier("openaiWebClient") WebClient openaiClient,
                                  AppProperties props) {
        this.openaiClient = openaiClient;
        this.props = props;
    }

    @Override
    public float[] embed(String text) {
        try {
            var body = mapper.createObjectNode();
            body.put("model", props.getEmbedding().getOpenai().getModel());
            body.put("input", text);

            String response = openaiClient.post()
                    .uri("")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = mapper.readTree(response);
            JsonNode arr = root.path("data").get(0).path("embedding");
            List<Float> vec = new ArrayList<>(arr.size());
            for (JsonNode v : arr) vec.add((float) v.asDouble());
            float[] out = new float[vec.size()];
            for (int i = 0; i < vec.size(); i++) out[i] = vec.get(i);
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Embedding provider (OpenAI) failed: " + e.getMessage(), e);
        }
    }
}
