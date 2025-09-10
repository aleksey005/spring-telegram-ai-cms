// /src/main/java/ru/simshp/qdrant/api/VectorController.java
package ru.simshp.qdrant.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.simshp.qdrant.api.dto.IngestResponse;
import ru.simshp.qdrant.api.dto.SearchRequest;
import ru.simshp.qdrant.api.dto.SearchResponse;
import ru.simshp.qdrant.config.AppProperties;
import ru.simshp.qdrant.service.VectorStoreService;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class VectorController {

    private final VectorStoreService service;
    private final AppProperties props;

    /**
     * Принимает произвольный JSON-объект и индексирует его.
     * Пример:
     * curl -X POST http://localhost:8080/ingest -H "Content-Type: application/json" -d '{"title":"Hello","body":"World"}'
     */
    @PostMapping(value = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public IngestResponse ingest(@RequestBody JsonNode body) {
        String id = service.ingest(body);
        return new IngestResponse(id, props.getQdrant().getCollection());
    }

    /**
     * Поиск семантически близких документов по текстовому запросу.
     * Пример:
     * curl -X POST http://localhost:8080/search -H "Content-Type: application/json" -d '{"query":"Hello","topK":5}'
     */
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SearchResponse search(@Valid @RequestBody SearchRequest req) {
        return service.search(req.query(), req.topK());
    }
}
