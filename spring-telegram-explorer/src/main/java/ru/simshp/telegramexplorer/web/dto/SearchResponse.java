package ru.simshp.telegramexplorer.web.dto;

import java.util.List;

public record SearchResponse(
        String query,
        List<MessageView> results
) {
}
