package ru.simshp.telegramexplorer.service;

import dev.voroby.springframework.telegram.client.TelegramClient;
import dev.voroby.springframework.telegram.client.templates.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.TdApi;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramFavoritesService {

    private final TelegramClient telegramClient;

    private final AtomicReference<Long> savedMessagesChatId = new AtomicReference<>();

    public void saveMessage(String author, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        long chatId;
        try {
            chatId = resolveSavedMessagesChatId();
        } catch (IllegalStateException ex) {
            log.warn("Cannot determine Saved Messages chat id: {}", ex.getMessage());
            return;
        }

        String formattedText = author == null || author.isBlank()
                ? text
                : "%s: %s".formatted(author.trim(), text);

        TdApi.InputMessageText input = new TdApi.InputMessageText(
                new TdApi.FormattedText(formattedText, null),
                null,
                false
        );

        Response<TdApi.Message> response = telegramClient.send(
                new TdApi.SendMessage(chatId, 0, null, null, null, input)
        );

        Optional<TdApi.Error> errorOpt = response.getError();
        if (errorOpt.isPresent()) {
            TdApi.Error error = errorOpt.get();
            log.warn("Failed to send message to Saved Messages: [{}] {}", error.code, error.message);
        }
    }

    private long resolveSavedMessagesChatId() {
        Long cached = savedMessagesChatId.get();
        if (cached != null) {
            return cached;
        }

        synchronized (savedMessagesChatId) {
            cached = savedMessagesChatId.get();
            if (cached != null) {
                return cached;
            }

            Response<TdApi.User> meResponse = telegramClient.send(new TdApi.GetMe());
            Optional<TdApi.Error> meError = meResponse.getError();
            if (meError.isPresent()) {
                TdApi.Error error = meError.get();
                throw new IllegalStateException("GetMe failed: [%d] %s".formatted(error.code, error.message));
            }

            TdApi.User me = meResponse.getObject()
                    .orElseThrow(() -> new IllegalStateException("GetMe returned empty result"));

            Response<TdApi.Chat> chatResponse = telegramClient.send(
                    new TdApi.CreatePrivateChat(me.id, false)
            );

            Optional<TdApi.Error> chatError = chatResponse.getError();
            if (chatError.isPresent()) {
                TdApi.Error error = chatError.get();
                throw new IllegalStateException("CreatePrivateChat failed: [%d] %s".formatted(error.code, error.message));
            }

            TdApi.Chat chat = chatResponse.getObject()
                    .orElseThrow(() -> new IllegalStateException("CreatePrivateChat returned empty result"));

            savedMessagesChatId.set(chat.id);
            return chat.id;
        }
    }
}
