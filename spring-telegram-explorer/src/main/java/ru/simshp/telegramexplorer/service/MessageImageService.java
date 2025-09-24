package ru.simshp.telegramexplorer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.simshp.telegramexplorer.domain.MediaEntity;
import ru.simshp.telegramexplorer.repo.MediaRepository;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageImageService {

    private static final String PHOTO_KIND = "photo";

    private final MediaRepository mediaRepository;

    public Optional<MessageImage> findFirstPhoto(Long messageId) {
        if (messageId == null) {
            return Optional.empty();
        }
        return mediaRepository.findFirstByMessage_IdAndKindOrderByIdAsc(messageId, PHOTO_KIND)
                .flatMap(this::toMessageImage);
    }

    private Optional<MessageImage> toMessageImage(MediaEntity media) {
        if (media == null) {
            return Optional.empty();
        }
        String filePath = media.getFilePath();
        if (filePath == null || filePath.isBlank()) {
            return Optional.empty();
        }
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path) || !Files.isReadable(path)) {
                return Optional.empty();
            }
            return Optional.of(new MessageImage(path, media.getMimeType()));
        } catch (InvalidPathException ex) {
            return Optional.empty();
        }
    }

    public record MessageImage(Path path, String mimeType) {
    }
}

