package ru.simshp.telegramexplorer.web;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.simshp.telegramexplorer.config.ExplorerProperties;
import ru.simshp.telegramexplorer.domain.AiCommentEntity;
import ru.simshp.telegramexplorer.repo.ChannelRepository;
import ru.simshp.telegramexplorer.repo.MessageRepository;
import ru.simshp.telegramexplorer.service.AiCommentService;
import ru.simshp.telegramexplorer.service.SearchService;
import ru.simshp.telegramexplorer.service.MessageImageService;
import ru.simshp.telegramexplorer.web.dto.AiCommentRequest;
import ru.simshp.telegramexplorer.web.dto.AiCommentResponse;
import ru.simshp.telegramexplorer.web.dto.ChannelView;
import ru.simshp.telegramexplorer.web.dto.MessageView;
import ru.simshp.telegramexplorer.web.dto.MessageViewMapper;
import ru.simshp.telegramexplorer.web.dto.PageResponse;
import ru.simshp.telegramexplorer.web.dto.SearchResponse;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Comparator;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageApiController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;
    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final SearchService searchService;
    private final MessageImageService messageImageService;
    private final MessageViewMapper messageViewMapper;
    private final ExplorerProperties explorerProperties;
    private final AiCommentService aiCommentService;

    @GetMapping("/messages")
    public PageResponse<MessageView> index(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
                                           @RequestParam(name = "channel", required = false) List<String> channelFilter) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "id")
        );
        var normalizedChannels = normalizeChannelFilter(channelFilter);
        var pageData = (normalizedChannels.isEmpty()
                ? messageRepository.findAllBy(pageable)
                : messageRepository.findAllByChannel_UsernameIn(normalizedChannels, pageable))
                .map(messageViewMapper::toView);
        return PageResponse.from(pageData);
    }

    @GetMapping("/messages/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Long id) {
        return messageImageService.findFirstPhoto(id)
                .map(this::toImageResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q) {
        var results = searchService.search(q);
        return new SearchResponse(q, results);
    }

    @PostMapping("/messages/{id}/ai-comment")
    public AiCommentResponse createAiComment(@PathVariable Long id,
                                             @RequestBody(required = false) AiCommentRequest request) {
        try {
            AiCommentEntity comment = aiCommentService.generateComment(id, request != null ? request.text() : null);
            return new AiCommentResponse(id, comment.getCommentText());
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found", ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Не удалось получить комментарий от модели", ex);
        }
    }

    @GetMapping("/channels")
    public List<ChannelView> channels() {
        var sort = Sort.by(Sort.Direction.ASC, "username");
        var channelsFromDb = channelRepository.findAll(sort);

        var result = new ArrayList<ChannelView>(channelsFromDb.size());
        var known = new LinkedHashSet<String>();

        for (var channel : channelsFromDb) {
            String username = channel.getUsername();
            result.add(new ChannelView(username, channel.getTitle()));
            if (username != null) {
                known.add(username.toLowerCase(Locale.ROOT));
            }
        }

        for (var configured : explorerProperties.getNormalizedChannelUsernames()) {
            if (known.add(configured)) {
                result.add(new ChannelView(configured, null));
            }
        }

        result.sort(Comparator.comparing(ChannelView::username, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ChannelView::username));
        return List.copyOf(result);
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private ResponseEntity<Resource> toImageResponse(MessageImageService.MessageImage image) {
        try {
            Resource resource = new UrlResource(image.path().toUri());
            MediaType mediaType = parseMediaType(image.mimeType());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private MediaType parseMediaType(String mimeType) {
        if (mimeType != null && !mimeType.isBlank()) {
            try {
                return MediaType.parseMediaType(mimeType);
            } catch (InvalidMediaTypeException ignored) {
            }
        }
        return MediaType.IMAGE_JPEG;
    }

    private List<String> normalizeChannelFilter(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String channel : channels) {
            if (channel == null) {
                continue;
            }
            String candidate = channel.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            if (candidate.startsWith("@")) {
                candidate = candidate.substring(1);
            }
            if (!candidate.isEmpty()) {
                normalized.add(candidate);
            }
        }
        return List.copyOf(normalized);
    }
}
