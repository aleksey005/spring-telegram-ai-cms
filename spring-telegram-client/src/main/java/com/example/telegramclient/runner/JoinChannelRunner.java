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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@Order(10)
public class JoinChannelRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JoinChannelRunner.class);

    private final Environment env;
    private final ApplicationContext ctx;
    private final ClientAuthorizationState auth;

    public static volatile Long targetChannelId = null;

    public JoinChannelRunner(Environment env, ApplicationContext ctx, ClientAuthorizationState auth) {
        this.env = env;
        this.ctx = ctx;
        this.auth = auth;
    }

    @Override
    public void run(ApplicationArguments args) {
        String channelUsername = env.getProperty("app.channel.username");
        if (!StringUtils.hasText(channelUsername)) {
            log.warn("app.channel.username не задан — будем реагировать на все каналы, где видим посты");
            return;
        }

        int attempts = 0;
        while (!auth.haveAuthorization() && attempts < 120) {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            attempts++;
        }
        if (!auth.haveAuthorization()) {
            log.warn("Не дождался авторизации TDLib — пропускаю поиск канала");
            return;
        }

        TelegramClient client = ctx.getBean(TelegramClient.class);

        var searchResp = client.send(new TdApi.SearchPublicChat(channelUsername));
        Optional<TdApi.Chat> chatOpt = searchResp.getObject();
        if (chatOpt.isEmpty()) {
            log.error("Канал @{} не найден", channelUsername);
            return;
        }
        TdApi.Chat chat = chatOpt.get();
        targetChannelId = chat.id;
        log.info("Найден канал @{}: id={}", channelUsername, chat.id);
    }
}
