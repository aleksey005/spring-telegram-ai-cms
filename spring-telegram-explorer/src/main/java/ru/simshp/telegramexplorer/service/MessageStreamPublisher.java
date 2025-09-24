package ru.simshp.telegramexplorer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.simshp.telegramexplorer.domain.MessageEntity;
import ru.simshp.telegramexplorer.web.dto.MessageViewMapper;
import ru.simshp.telegramexplorer.web.websocket.MessageUpdatesWebSocketHandler;

@Service
@RequiredArgsConstructor
public class MessageStreamPublisher {

    private final MessageViewMapper messageViewMapper;
    private final MessageUpdatesWebSocketHandler webSocketHandler;

    public void publishCreatedMessage(MessageEntity entity) {
        if (entity == null) {
            return;
        }
        webSocketHandler.sendMessageCreated(messageViewMapper.toView(entity));
    }

    public void publishUpdatedMessage(MessageEntity entity) {
        if (entity == null) {
            return;
        }
        webSocketHandler.sendMessageUpdated(messageViewMapper.toView(entity));
    }
}
