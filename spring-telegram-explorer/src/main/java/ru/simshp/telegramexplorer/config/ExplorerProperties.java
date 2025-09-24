package ru.simshp.telegramexplorer.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter @Setter
@Validated
@ConfigurationProperties(prefix = "explorer")
public class ExplorerProperties {

    /** Список каналов: usernames через запятую в application.yaml */
    private String channels;

    /** Директория для медиа (volume) */
    @NotBlank
    private String mediaDir;

    private boolean downloadPhotos = true;

    /** Разрешенные форматы загрузки фото */
    private List<String> downloadFormats = List.of("png","jpg","jpeg","webp");

    private OpenAiProps openai = new OpenAiProps();

    /**
     * Возвращает нормализованный список usernames каналов из конфигурации.
     * Дополнительно обрезает возможные приставки вида {@code @} или {@code https://t.me/}.
     */
    public Set<String> getNormalizedChannelUsernames() {
        if (channels == null || channels.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(channels.split(","))
                .map(String::trim)
                .map(ExplorerProperties::normalizeChannelUsername)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Optional<String> normalizeChannelUsername(String raw) {
        if (raw == null) {
            return Optional.empty();
        }

        String value = raw.trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }

        String lowerCase = value.toLowerCase(Locale.ROOT);
        if (lowerCase.startsWith("https://t.me/")) {
            value = value.substring("https://t.me/".length());
        } else if (lowerCase.startsWith("http://t.me/")) {
            value = value.substring("http://t.me/".length());
        } else if (lowerCase.startsWith("t.me/")) {
            value = value.substring("t.me/".length());
        }

        if (value.startsWith("@")) {
            value = value.substring(1);
        }

        int slashIdx = value.indexOf('/');
        if (slashIdx >= 0) {
            value = value.substring(0, slashIdx);
        }

        int queryIdx = value.indexOf('?');
        if (queryIdx >= 0) {
            value = value.substring(0, queryIdx);
        }

        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    @Getter @Setter
    public static class OpenAiProps {
        @NotBlank
        private String apiKey;
        @NotBlank
        private String embeddingsModel = "text-embedding-3-small";
        private int dimensions = 1536;
        @NotBlank
        private String commentModel = "gpt-4o-mini";
    }
}
