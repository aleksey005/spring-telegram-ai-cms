package ru.simshp.telegramexplorer.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.simshp.telegramexplorer.service.AiAssistantService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiAssistantWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final AiAssistantService assistantService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String greeting = assistantService.startSession(session.getId());
        sendAssistantMessage(session, greeting);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        assistantService.endSession(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        try {
            ClientMessage clientMessage = objectMapper.readValue(payload, ClientMessage.class);
            if (!"user-message".equals(clientMessage.type())) {
                return;
            }

            assistantService.handleUserMessage(session.getId(), clientMessage.text())
                    .ifPresent(response -> sendAssistantMessage(session, response));
        } catch (IOException ex) {
            log.warn("Failed to parse assistant message: {}", ex.getMessage());
            sendError(session, "Не удалось обработать сообщение. Попробуйте снова." );
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("Assistant WebSocket error: {}", exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    private void sendAssistantMessage(WebSocketSession session, String text) {
        if (!session.isOpen()) {
            return;
        }
        try {
            ServerMessage serverMessage = new ServerMessage("assistant-message", "assistant", text);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(serverMessage)));
        } catch (IOException ex) {
            log.warn("Failed to send assistant message: {}", ex.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String text) {
        if (!session.isOpen()) {
            return;
        }
        try {
            ServerMessage serverMessage = new ServerMessage("assistant-message", "system", text);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(serverMessage)));
        } catch (IOException ex) {
            log.warn("Failed to send assistant error: {}", ex.getMessage());
        }
    }

    private record ClientMessage(String type, String text) {
    }

    private record ServerMessage(String type, String role, String text) {
    }
}
