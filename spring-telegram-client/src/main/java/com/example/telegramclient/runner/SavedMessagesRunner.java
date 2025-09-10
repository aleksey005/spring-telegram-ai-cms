package com.example.telegramclient.runner;

import dev.voroby.springframework.telegram.client.TelegramClient;
import dev.voroby.springframework.telegram.client.updates.ClientAuthorizationState;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(5)
public class SavedMessagesRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SavedMessagesRunner.class);

    private final ApplicationContext ctx;
    private final ClientAuthorizationState auth;

    public static volatile Long savedMessagesChatId = null;
    public static volatile Long myUserId = null;

    public SavedMessagesRunner(ApplicationContext ctx, ClientAuthorizationState auth) {
        this.ctx = ctx;
        this.auth = auth;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Ждём авторизацию TDLib
        int attempts = 0;
        while (!auth.haveAuthorization() && attempts < 120) {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            attempts++;
        }
        if (!auth.haveAuthorization()) {
            log.warn("Не дождался авторизации TDLib — не могу определить Saved Messages");
            return;
        }

        TelegramClient client = ctx.getBean(TelegramClient.class);

        // Получаем себя
        var meResp = client.send(new TdApi.GetMe());
        Optional<TdApi.User> meOpt = meResp.getObject();
        if (meOpt.isEmpty()) {
            log.error("GetMe вернул пусто");
            return;
        }
        TdApi.User me = meOpt.get();
        myUserId = me.id;

        // Создаём/получаем приватный чат с самим собой (Saved Messages)
        var chatResp = client.send(new TdApi.CreatePrivateChat(me.id, true));
        Optional<TdApi.Chat> chatOpt = chatResp.getObject();
        if (chatOpt.isEmpty()) {
            log.error("CreatePrivateChat(me) не вернул Chat");
            return;
        }
        savedMessagesChatId = chatOpt.get().id;
        log.info("Saved Messages chatId = {}", savedMessagesChatId);
    }
}
