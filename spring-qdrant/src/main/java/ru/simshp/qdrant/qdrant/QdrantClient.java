// /src/main/java/ru/simshp/qdrant/qdrant/QdrantClient.java
package ru.simshp.qdrant.qdrant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.simshp.qdrant.config.AppProperties;

import java.util.ArrayList;
import java.util.List;

@Component
public class QdrantClient {

    private final WebClient qdrant;
    private final AppProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public QdrantClient(@Qualifier("qdrantWebClient") WebClient qdrant,
                        AppProperties props) {
        this.qdrant = qdrant;
        this.props = props;
    }

    public void ensureCollection() {
        if (!props.getQdrant().isCreateIfMissing()) return;

        var vectors = mapper.createObjectNode();
        vectors.put("size", props.getQdrant().getVectorSize());
        vectors.put("distance", props.getQdrant().getDistance());

        var req = mapper.createObjectNode();
        req.set("vectors", vectors);

        qdrant.put()
                .uri("/collections/{name}", props.getQdrant().getCollection())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> reactor.core.publisher.Mono.just("ok"))
                .block();
    }

    public String upsertJson(String id, float[] vector, JsonNode payloadJson) {
        try {
            var point = mapper.createObjectNode();
            point.put("id", id);

            var vecArr = mapper.createArrayNode();
            for (float v : vector) vecArr.add(v);
            point.set("vector", vecArr);

            var payload = mapper.createObjectNode();
            payload.set("raw", payloadJson);
            point.set("payload", payload);

            var root = mapper.createObjectNode();
            root.set("points", mapper.createArrayNode().add(point));

            return qdrant.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/collections/{name}/points")
                            .queryParam("wait", "true")
                            .build(props.getQdrant().getCollection()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(root)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Qdrant upsert failed: " + e.getMessage(), e);
        }
    }

    public List<SearchHitInternal> search(float[] vector, int topK) {
        try {
            var vecArr = mapper.createArrayNode();
            for (float v : vector) vecArr.add(v);

            var req = mapper.createObjectNode();
            req.set("vector", vecArr);
            req.put("limit", topK);
            req.put("with_payload", true);
            req.put("with_vector", false);

            String resp = qdrant.post()
                    .uri("/collections/{name}/points/search", props.getQdrant().getCollection())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            var root = mapper.readTree(resp);
            var out = new ArrayList<SearchHitInternal>();
            for (var r : root.path("result")) {
                double score = r.path("score").asDouble();
                String id = r.path("id").asText();
                var payload = r.path("payload").path("raw");
                out.add(new SearchHitInternal(score, id, payload));
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Qdrant search failed: " + e.getMessage(), e);
        }
    }

    public record SearchHitInternal(double score, String id, JsonNode payload) {}
}
