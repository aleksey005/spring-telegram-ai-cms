package ru.simshp.telegramexplorer.web;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.simshp.telegramexplorer.repo.MessageRepository;
import ru.simshp.telegramexplorer.service.SearchService;
import ru.simshp.telegramexplorer.web.dto.MessageView;
import ru.simshp.telegramexplorer.web.dto.PageResponse;
import ru.simshp.telegramexplorer.web.dto.SearchResponse;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageApiController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    private final MessageRepository messageRepository;
    private final SearchService searchService;

    @GetMapping("/messages")
    public PageResponse<MessageView> index(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "id")
        );
        var pageData = messageRepository.findAllBy(pageable).map(m ->
                new MessageView(
                        m.getId(),
                        m.getChannel() != null ? m.getChannel().getUsername() : "",
                        m.isComment(),
                        m.getThreadId(),
                        m.getAuthorUsername(),
                        m.getText(),
                        m.getCaption(),
                        m.isHasMedia(),
                        m.getPublishedAt()
                )
        );
        return PageResponse.from(pageData);
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q) {
        var results = searchService.search(q);
        return new SearchResponse(q, results);
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
