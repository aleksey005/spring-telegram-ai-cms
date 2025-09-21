package ru.simshp.telegramexplorer.web;

import dev.voroby.springframework.telegram.client.TelegramClient;
import dev.voroby.springframework.telegram.client.templates.response.Response;
import jakarta.validation.Valid;
import org.drinkless.tdlib.TdApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.simshp.telegramexplorer.web.dto.TelegramAuthCodeRequest;

import java.util.Map;

@RestController
@RequestMapping("/telegram/auth")
@Validated
public class TelegramAuthController {

    private final TelegramClient telegramClient;

    public TelegramAuthController(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @PostMapping("/code")
    public ResponseEntity<?> submitCode(@Valid @RequestBody TelegramAuthCodeRequest request) {
        try {
            Response<TdApi.Ok> response = telegramClient.send(new TdApi.CheckAuthenticationCode(request.code()));
            if (response.getError().isPresent()) {
                TdApi.Error error = response.getError().get();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "error", "Failed to verify authentication code",
                                "details", error.message
                        ));
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Unexpected error while verifying authentication code",
                            "details", e.getMessage()
                    ));
        }
    }
}
