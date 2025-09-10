// /src/main/java/ru/simshp/qdrant/api/dto/SearchHit.java
package ru.simshp.qdrant.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record SearchHit(double score, JsonNode payload, String id) {}
