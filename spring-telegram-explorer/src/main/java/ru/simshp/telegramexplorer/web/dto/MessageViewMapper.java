package ru.simshp.telegramexplorer.web.dto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.simshp.telegramexplorer.domain.MessageEntity;
import ru.simshp.telegramexplorer.service.MessageImageService;

@Component
@RequiredArgsConstructor
public class MessageViewMapper {

    private static final String IMAGE_ENDPOINT_TEMPLATE = "/api/messages/%d/image";

    private final MessageImageService messageImageService;

    public MessageView toView(MessageEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Message entity must not be null");
        }

        return new MessageView(
                entity.getId(),
                entity.getChannel() != null ? entity.getChannel().getUsername() : "",
                entity.isComment(),
                entity.getThreadId(),
                entity.getAuthorUsername(),
                entity.getText(),
                entity.getCaption(),
                entity.isHasMedia(),
                buildImageUrl(entity.getId()),
                entity.getAiComment() != null ? entity.getAiComment().getCommentText() : null,
                entity.getPublishedAt()
        );
    }

    private String buildImageUrl(Long messageId) {
        if (messageId == null) {
            return null;
        }
        return messageImageService.findFirstPhoto(messageId)
                .map(image -> IMAGE_ENDPOINT_TEMPLATE.formatted(messageId))
                .orElse(null);
    }
}
