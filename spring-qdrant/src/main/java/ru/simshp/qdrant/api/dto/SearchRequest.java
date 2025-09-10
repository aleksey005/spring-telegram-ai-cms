// /src/main/java/ru/simshp/qdrant/api/dto/SearchRequest.java
package ru.simshp.qdrant.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank String query,
        @Min(1) int topK
) {
    public SearchRequest {
        if (topK == 0) topK = 5;
    }
}
