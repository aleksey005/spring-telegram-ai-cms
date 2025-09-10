package com.example.telegramclient.service;

import com.example.telegramclient.openai.OpenAiClient;
import dev.voroby.springframework.telegram.client.TelegramClient;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);
    private final OpenAiClient openAi;

    public CommentService(OpenAiClient openAi) {
        this.openAi = openAi;
    }

    public boolean isFresh(long messageUnixTime, long freshWindowSeconds) {
        long now = Instant.now().getEpochSecond();
        return (now - messageUnixTime) <= freshWindowSeconds;
    }

    public Optional<String> buildCaptionForMessagePhoto(TelegramClient client, TdApi.MessagePhoto mp) {
        try {
            TdApi.Photo photo = mp.photo;
            TdApi.PhotoSize best = null;
            for (TdApi.PhotoSize ps : photo.sizes) {
                if (best == null) best = ps;
                else {
                    long bestBytes = best.photo.size;
                    long curBytes  = ps.photo.size;
                    if (curBytes > bestBytes) best = ps;
                }
            }
            if (best == null) return Optional.empty();

            String path = downloadFileAndGetPath(client, best.photo.id);
            if (path == null || path.isBlank()) {
                log.warn("Не удалось скачать фото id={}", best.photo.id);
                return Optional.empty();
            }
            byte[] bytes = Files.readAllBytes(Path.of(path));
            String mime = guessMime(path);

            String caption = openAi.captionImage(bytes, mime);
            caption = caption.trim();
            if (caption.length() > 1024) caption = caption.substring(0, 1024).trim();
            return Optional.of(caption);
        } catch (Exception e) {
            log.error("Ошибка генерации комментария: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /** Простой текст в чат (в т.ч. в Saved Messages). */
    public Optional<Long> sendPlainText(TelegramClient client, long chatId, String text) {
        TdApi.FormattedText ft = new TdApi.FormattedText();
        ft.text = text;

        TdApi.InputMessageText content = new TdApi.InputMessageText();
        content.text = ft;

        TdApi.LinkPreviewOptions lpo = new TdApi.LinkPreviewOptions();
        lpo.isDisabled = true;
        content.linkPreviewOptions = lpo;
        content.clearDraft = true;

        TdApi.InputMessageReplyTo replyTo = null;
        TdApi.MessageSendOptions options = null;
        TdApi.ReplyMarkup replyMarkup = null;

        TdApi.SendMessage send = new TdApi.SendMessage(
                chatId,
                0,            // без тем
                replyTo,
                options,
                replyMarkup,
                content
        );
        var resp = client.send(send);
        Optional<Long> idOpt = resp.getObject().map(m -> m.id);
        idOpt.ifPresent(id -> log.info("Текст отправлен, messageId={}", id));
        resp.getError().ifPresent(e -> log.error("SendMessage error: {} ({})", e.message, e.code));
        return idOpt;
    }

    private String downloadFileAndGetPath(TelegramClient client, int fileId) throws InterruptedException {
        TdApi.DownloadFile d = new TdApi.DownloadFile(fileId, 32, 0L, 0L, true);
        client.send(d);

        for (int i = 0; i < 50; i++) {
            var fresp = client.send(new TdApi.GetFile(fileId));
            Optional<TdApi.File> fo = fresp.getObject();
            if (fo.isPresent()) {
                TdApi.File f = fo.get();
                if (f.local != null && f.local.isDownloadingCompleted) {
                    return f.local.path;
                }
            }
            Thread.sleep(200);
        }
        var last = client.send(new TdApi.GetFile(fileId)).getObject();
        return last.map(f -> f.local != null ? f.local.path : null).orElse(null);
    }

    private String guessMime(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "image/jpeg";
    }
}
