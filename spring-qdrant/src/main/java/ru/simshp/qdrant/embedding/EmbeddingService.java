// /src/main/java/ru/simshp/qdrant/embedding/EmbeddingService.java
package ru.simshp.qdrant.embedding;

public interface EmbeddingService {
    float[] embed(String text);
}
