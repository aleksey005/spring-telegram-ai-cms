package ru.simshp.telegramexplorer.web.dto;

import java.time.OffsetDateTime;

public record MessageView(
        Long id, String channel, boolean comment, Long threadId,
        String author, String text, String caption, boolean hasMedia,
        OffsetDateTime publishedAt) {}
