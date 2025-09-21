package ru.simshp.telegramexplorer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.simshp.telegramexplorer.config.ExplorerProperties;

import java.io.IOException;
import java.nio.file.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaStorageService {

    private final ExplorerProperties props;

    public Path ensureMediaDir() throws IOException {
        var dir = Path.of(props.getMediaDir()).toAbsolutePath();
        Files.createDirectories(dir);
        return dir;
    }

    public Path saveBytes(byte[] bytes, String suggestedName) throws IOException {
        var dir = ensureMediaDir();
        var p = dir.resolve(suggestedName);
        Files.write(p, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("Saved media: {}", p);
        return p;
    }
}
