package ru.simshp.telegramexplorer.tdlib;

import dev.voroby.springframework.telegram.TelegramRunner;
import dev.voroby.springframework.telegram.client.TelegramClient;
import org.drinkless.tdlib.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.simshp.telegramexplorer.config.ExplorerProperties;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class BootstrapRunner implements TelegramRunner {

    private final TelegramClient client;
    private final ExplorerProperties props;

    @Override
    public void run(ApplicationArguments args) {
        log.info("TDLib authorized. Bootstrap loading chats for channels list…");
        var usernames = props.getNormalizedChannelUsernames();
        if (usernames.isEmpty()) return;

        for (var username : usernames) {
            try {
                var searchResp = client.send(new TdApi.SearchPublicChat(username));
                if (searchResp.getError().isPresent()) {
                    var error = searchResp.getError().get();
                    log.warn("Channel lookup for {} failed: [{}] {}", username, error.code, error.message);
                    continue;
                }

                var chat = searchResp.getObject().orElse(null);
                if (chat == null) {
                    log.warn("Channel not found by username: {}", username);
                    continue;
                }
                var historyResp = client.send(new TdApi.GetChatHistory(chat.id, 0, 0, 100, false));
                historyResp.getObject().ifPresent(history ->
                        log.info("Boot history for {}: {} messages", username, history.totalCount)
                );
            } catch (Exception e) {
                log.warn("Bootstrap error for {}: {}", username, e.getMessage());
            }
        }
    }
}
