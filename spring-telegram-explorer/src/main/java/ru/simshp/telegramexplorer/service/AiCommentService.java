package ru.simshp.telegramexplorer.service;

import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.simshp.telegramexplorer.config.ExplorerProperties;
import ru.simshp.telegramexplorer.domain.AiCommentEntity;
import ru.simshp.telegramexplorer.domain.MessageEntity;
import ru.simshp.telegramexplorer.repo.AiCommentRepository;
import ru.simshp.telegramexplorer.repo.MessageRepository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCommentService {

    public static final String CENSORSHIP_RESPONSE = "цензура, нет комментария.";
    private static final Duration OPENAI_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_TOKENS = 400;
    private static final double TEMPERATURE = 0.7;
    private static final String SYSTEM_PROMPT = "Ты — вежливый русскоязычный ассистент, который пишет комментарии "
            + "к сообщениям из публичных Telegram-каналов. "
            + "Ответ должен состоять из 4–5 предложений, быть политкорректным и конструктивным. "
            + "Ты должен проявить аналитический подход и провести непредвзятый анализ сообщения. "
            + "Так же классифицируй сообщение путем создания тегов в конце твоего комментария. "
            + "Если исходный текст нарушает политику безопасности, содержит жесткий мат или невозможно ответить "
            + "без нарушения правил, ответь строго фразой \"" + CENSORSHIP_RESPONSE + "\".";

    private final ExplorerProperties properties;
    private final MessageRepository messageRepository;
    private final AiCommentRepository aiCommentRepository;
    private final MessageStreamPublisher messageStreamPublisher;

    @Transactional
    public AiCommentEntity generateComment(Long messageId, String overrideText) {
        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        String sourceText = normalizeText(overrideText);
        if (sourceText == null) {
            sourceText = normalizeText(message.getText());
        }
        if (sourceText == null) {
            sourceText = normalizeText(message.getCaption());
        }

        if (sourceText == null) {
            log.debug("Message {} has no text to comment", messageId);
            return saveComment(message, "Комментарий недоступен: нет текста для анализа.");
        }

        log.info("Requesting AI comment for message {} with text: {}", messageId, abbreviateForLog(sourceText));

        String commentText;
        try {
            commentText = requestCommentFromOpenAi(sourceText);
            log.info("AI response for message {}: {}", messageId, abbreviateForLog(commentText));
        } catch (ContentFilteredException ex) {
            commentText = CENSORSHIP_RESPONSE;
            log.info("AI response for message {} is filtered: {}", messageId, commentText);
        }

        return saveComment(message, commentText);
    }

    private AiCommentEntity saveComment(MessageEntity message, String commentText) {
        AiCommentEntity comment = aiCommentRepository.findByMessage_Id(message.getId())
                .orElseGet(AiCommentEntity::new);
        comment.setMessage(message);
        comment.setCommentText(commentText);
        comment.setCreatedAt(OffsetDateTime.now());

        AiCommentEntity saved = aiCommentRepository.save(comment);
        message.setAiComment(saved);
        messageStreamPublisher.publishUpdatedMessage(message);
        return saved;
    }

    private String requestCommentFromOpenAi(String sourceText) {
        var client = new OpenAiService(properties.getOpenai().getApiKey(), OPENAI_TIMEOUT);
        var request = ChatCompletionRequest.builder()
                .model(properties.getOpenai().getCommentModel())
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .messages(List.of(
                        new ChatMessage("system", SYSTEM_PROMPT),
                        new ChatMessage("user", sourceText)
                ))
                .build();

        var response = client.createChatCompletion(request);
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new IllegalStateException("OpenAI returned an empty response");
        }

        ChatCompletionChoice choice = response.getChoices().get(0);
        if (choice == null) {
            throw new IllegalStateException("OpenAI returned a null choice");
        }

        if ("content_filter".equalsIgnoreCase(choice.getFinishReason())) {
            throw new ContentFilteredException();
        }

        var message = choice.getMessage();
        if (message == null) {
            throw new IllegalStateException("OpenAI returned a null message");
        }

        String content = normalizeText(message.getContent());
        if (content == null) {
            throw new IllegalStateException("OpenAI returned an empty comment");
        }

        if (CENSORSHIP_RESPONSE.equalsIgnoreCase(content.trim())) {
            return CENSORSHIP_RESPONSE;
        }

        return content;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String abbreviateForLog(String value) {
        if (value == null) {
            return null;
        }
        int maxLength = 200;
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "…";
    }

    private static class ContentFilteredException extends RuntimeException {
    }
}
