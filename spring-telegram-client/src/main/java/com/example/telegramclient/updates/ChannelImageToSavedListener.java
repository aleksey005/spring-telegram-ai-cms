package com.example.telegramclient.updates;

import com.example.telegramclient.runner.JoinChannelRunner;
import com.example.telegramclient.service.CommentService;
import com.example.telegramclient.publish.ExternalTextPublisher;
import com.example.telegramclient.publish.ExternalIngestPublisher;
import com.example.telegramclient.publish.ImagePublisher;
import dev.voroby.springframework.telegram.client.TelegramClient;
import dev.voroby.springframework.telegram.client.updates.UpdateNotificationListener;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Executor;

@Component
public class ChannelImageToSavedListener implements UpdateNotificationListener<TdApi.UpdateNewMessage> {

    private static final Logger log = LoggerFactory.getLogger(ChannelImageToSavedListener.class);

    private final CommentService commentService;
    private final ExternalTextPublisher publisher; // <-- сервис публикации в text-url
    private final ExternalIngestPublisher ingestPublisher; // <-- сервис публикации в ingest
    private final ImagePublisher imagePublisher; // <-- сервис публикации изображения
    private final long freshWindowSec;
    private final ApplicationContext ctx;
    private final Executor tgExecutor;

    public ChannelImageToSavedListener(
            CommentService commentService,
            ExternalTextPublisher publisher,
            ExternalIngestPublisher ingestPublisher,
            ImagePublisher imagePublisher,
            Environment env,
            ApplicationContext ctx,
            @Qualifier("tgExecutor") Executor tgExecutor
    ) {
        this.commentService = commentService;
        this.publisher = publisher;
        this.ingestPublisher = ingestPublisher;
        this.imagePublisher = imagePublisher;
        this.freshWindowSec = env.getProperty("app.channel.fresh-window-seconds", Long.class, 60L);
        this.ctx = ctx;
        this.tgExecutor = tgExecutor;
    }

    @Override
    public void handleNotification(TdApi.UpdateNewMessage upd) {
        TdApi.Message msg = upd.message;

        // быстрые фильтры — безопасно делать в TDLib thread
        if (!msg.isChannelPost) return;
        if (!(msg.content instanceof TdApi.MessagePhoto)) return;

        Long channelId = JoinChannelRunner.targetChannelId;
        if (channelId != null && msg.chatId != channelId) return;

        if (!commentService.isFresh(msg.date, freshWindowSec)) return;

        // копируем необходимые поля и уходим в воркер-пул
        final long messageId = msg.id;
        final TdApi.MessagePhoto photo = (TdApi.MessagePhoto) msg.content;
        tgExecutor.execute(() -> processInWorker(messageId, photo));
    }

    private static volatile Long savedMessagesChatIdCache = null;

    private void processInWorker(long messageId, TdApi.MessagePhoto photo) {
        try {
            TelegramClient client = ctx.getBean(TelegramClient.class);

            // 1) Сгенерировать текст
            Optional<String> opt = commentService.buildCaptionForMessagePhoto(client, photo);
            if (opt.isEmpty()) {
                log.info("Не удалось сгенерировать комментарий (msgId={})", messageId);
                return;
            }
            String caption = opt.get();

            // 2) Отправить в «Избранное» (если доступно)
            long savedId = resolveSavedMessagesChatId(client);
            long sentMessageId = 0;
            if (savedId != 0) {
                sentMessageId = commentService.sendPlainText(client, savedId, caption).orElse(0L);
                log.info("Сохранён коммент в Избранное (chatId={})", savedId);
            } else {
                log.warn("Saved Messages chatId неизвестен — пропускаю отправку в Избранное");
            }

            // 3) Параллельно отправить на внешние точки
            publisher.publishText(caption);
            ingestPublisher.ingestText(caption, sentMessageId);
            // Используем тот же messageId, что вернулся при публикации текста
            imagePublisher.publishPhoto(client, photo, sentMessageId);

        } catch (Exception ex) {
            log.error("Ошибка обработки сообщения {} в воркере: {}", messageId, ex.toString(), ex);
        }
    }

    private long resolveSavedMessagesChatId(TelegramClient client) {
        Long cached = savedMessagesChatIdCache;
        if (cached != null && cached != 0) return cached;

        synchronized (ChannelImageToSavedListener.class) {
            if (savedMessagesChatIdCache != null && savedMessagesChatIdCache != 0) {
                return savedMessagesChatIdCache;
            }
            var meResp = client.send(new TdApi.GetMe());
            Optional<TdApi.User> meOpt = meResp.getObject();
            if (meOpt.isEmpty()) {
                log.error("GetMe вернул пусто");
                return 0;
            }
            long myId = meOpt.get().id;

            var chatResp = client.send(new TdApi.CreatePrivateChat(myId, true));
            Optional<TdApi.Chat> chatOpt = chatResp.getObject();
            if (chatOpt.isEmpty()) {
                log.error("CreatePrivateChat(me) не вернул Chat");
                return 0;
            }
            savedMessagesChatIdCache = chatOpt.get().id;
            return savedMessagesChatIdCache;
        }
    }

    @Override
    public Class<TdApi.UpdateNewMessage> notificationType() {
        return TdApi.UpdateNewMessage.class;
    }
}
