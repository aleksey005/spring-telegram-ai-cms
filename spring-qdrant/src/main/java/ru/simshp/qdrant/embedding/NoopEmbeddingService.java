// /src/main/java/ru/simshp/qdrant/embedding/NoopEmbeddingService.java
package ru.simshp.qdrant.embedding;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("noop-embed")
@Primary
public class NoopEmbeddingService implements EmbeddingService {
    @Override
    public float[] embed(String text) {
        throw new IllegalStateException("Embedding provider is disabled. Configure app.embedding.*");
    }
}
