package ru.simshp.telegramexplorer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAssistantService {

    private static final Set<String> CHANNEL_KEYWORDS = Set.of("канал", "channel", "чат", "group");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?\\d[\\s-]?){6,}");

    private final TelegramFavoritesService telegramFavoritesService;

    private final Map<String, ConversationState> sessions = new ConcurrentHashMap<>();

    public String startSession(String sessionId) {
        ConversationState state = sessions.computeIfAbsent(sessionId, ConversationState::new);
        String greeting = "Здравствуйте! Я помогу подключить ваши каналы к мониторингу Telegram Explorer. " +
                "Расскажите, какие источники вы хотите отслеживать.";
        saveAssistantMessage(state, greeting);
        return greeting;
    }

    public Optional<String> handleUserMessage(String sessionId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }

        ConversationState state = sessions.computeIfAbsent(sessionId, ConversationState::new);
        saveUserMessage(state, userMessage);

        String response;
        if (!state.isChannelsCaptured()) {
            if (containsChannelInfo(userMessage)) {
                state.markChannelsCaptured();
                response = "Спасибо! Мы добавим эти каналы в очередь на подключение. " +
                        "Оставьте, пожалуйста, контакт для связи, и я расскажу о тарифах.";
            } else {
                response = "Чтобы я передал заявку, пришлите ссылки на каналы или их @usernames. " +
                        "Также могу подсказать по стоимости и возможностям решения.";
            }
        } else if (!state.isContactCaptured()) {
            Optional<String> contact = extractContact(userMessage);
            if (contact.isPresent()) {
                state.markContactCaptured(contact.get());
                response = "Отлично, записал контакт %s. Наш менеджер свяжется с вами и расскажет, " +
                        "как приобрести Telegram Explorer и подключить мониторинг.".formatted(contact.get());
            } else {
                response = "Не нашли контакт. Напишите телефон, email или ссылку на мессенджер, " +
                        "чтобы мы подготовили коммерческое предложение.";
            }
        } else {
            response = "Если нужны дополнительные возможности (аналитика, отчеты, API), дайте знать — " +
                    "мы поможем настроить и расскажем о вариантах приобретения.";
        }

        saveAssistantMessage(state, response);
        return Optional.of(response);
    }

    public void endSession(String sessionId) {
        sessions.remove(sessionId);
    }

    private void saveUserMessage(ConversationState state, String text) {
        try {
            telegramFavoritesService.saveMessage("Клиент", text);
        } catch (Exception ex) {
            log.warn("Failed to save user message for session {}: {}", state.id(), ex.getMessage());
        }
    }

    private void saveAssistantMessage(ConversationState state, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            telegramFavoritesService.saveMessage("Ассистент", text);
        } catch (Exception ex) {
            log.warn("Failed to save assistant message for session {}: {}", state.id(), ex.getMessage());
        }
    }

    private boolean containsChannelInfo(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("t.me/")) {
            return true;
        }
        if (lower.contains("http://") || lower.contains("https://")) {
            return true;
        }
        if (lower.contains("@")) {
            return true;
        }
        return CHANNEL_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private Optional<String> extractContact(String message) {
        var emailMatcher = EMAIL_PATTERN.matcher(message);
        if (emailMatcher.find()) {
            return Optional.of(emailMatcher.group());
        }
        var phoneMatcher = PHONE_PATTERN.matcher(message);
        if (phoneMatcher.find()) {
            return Optional.of(phoneMatcher.group());
        }
        if (message.contains("@")) {
            String trimmed = message.trim();
            if (trimmed.startsWith("@")) {
                return Optional.of(trimmed.split("\\s")[0]);
            }
        }
        return Optional.empty();
    }

    private static final class ConversationState {
        private final String id;
        private volatile boolean channelsCaptured;
        private volatile boolean contactCaptured;
        private volatile String contact;

        ConversationState(String id) {
            this.id = id;
        }

        void markChannelsCaptured() {
            this.channelsCaptured = true;
        }

        void markContactCaptured(String contact) {
            this.contactCaptured = true;
            this.contact = contact;
        }

        boolean isChannelsCaptured() {
            return channelsCaptured;
        }

        boolean isContactCaptured() {
            return contactCaptured;
        }

        String id() {
            return id;
        }
    }
}
