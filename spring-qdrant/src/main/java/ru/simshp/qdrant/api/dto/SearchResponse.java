// /src/main/java/ru/simshp/qdrant/api/dto/SearchResponse.java
package ru.simshp.qdrant.api.dto;

import java.util.List;

public record SearchResponse(List<SearchHit> hits) {}
