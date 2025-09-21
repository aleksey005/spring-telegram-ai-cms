package ru.simshp.telegramexplorer.tdlib;

import dev.voroby.springframework.telegram.client.updates.UpdateNotificationListener;
import org.drinkless.tdlib.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import ru.simshp.telegramexplorer.service.TelegramIngestService;

@Component
@Slf4j
public class UpdateNewMessageListener implements UpdateNotificationListener<TdApi.UpdateNewMessage> {

    private final TelegramIngestService ingestService;
    private final TaskExecutor telegramUpdateExecutor;

    public UpdateNewMessageListener(TelegramIngestService ingestService,
                                    @Qualifier("telegramUpdateExecutor") TaskExecutor telegramUpdateExecutor) {
        this.ingestService = ingestService;
        this.telegramUpdateExecutor = telegramUpdateExecutor;
    }

    @Override
    public void handleNotification(TdApi.UpdateNewMessage notification) {
        telegramUpdateExecutor.execute(() -> {
            try {
                ingestService.handleUpdateMessage(notification.message);
            } catch (Exception e) {
                log.error("Failed to process message: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public Class<TdApi.UpdateNewMessage> notificationType() {
        return TdApi.UpdateNewMessage.class;
    }
}
