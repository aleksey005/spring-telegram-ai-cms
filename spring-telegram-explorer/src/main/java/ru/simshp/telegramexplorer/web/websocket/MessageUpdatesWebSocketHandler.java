package ru.simshp.telegramexplorer.web.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.simshp.telegramexplorer.web.dto.MessageView;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageUpdatesWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessions.remove(session);
        log.debug("WebSocket transport error: {}", exception.getMessage(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void sendMessageCreated(MessageView messageView) {
        sendMessage("message-created", messageView);
    }

    public void sendMessageUpdated(MessageView messageView) {
        sendMessage("message-updated", messageView);
    }

    private void sendMessage(String type, MessageView messageView) {
        if (messageView == null) {
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(new MessageUpdatePayload(type, messageView));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize message update: {}", ex.getMessage(), ex);
            return;
        }

        TextMessage textMessage = new TextMessage(payload);
        sessions.removeIf(session -> !session.isOpen());

        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(textMessage);
            } catch (IOException ex) {
                log.debug("Failed to send update to session {}: {}", session.getId(), ex.getMessage());
                closeQuietly(session, CloseStatus.PROTOCOL_ERROR);
            }
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException closeEx) {
            log.debug("Failed to close WebSocket session {}: {}", session.getId(), closeEx.getMessage());
        }
    }

    private record MessageUpdatePayload(String type, MessageView message) {
    }
}
