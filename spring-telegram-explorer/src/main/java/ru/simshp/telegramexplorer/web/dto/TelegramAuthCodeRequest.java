package ru.simshp.telegramexplorer.web.dto;

import jakarta.validation.constraints.NotBlank;

public record TelegramAuthCodeRequest(
        @NotBlank(message = "Code must not be blank")
        String code
) {
}
