package com.example.tgpublisher.controller;

import com.example.tgpublisher.service.TelegramApiModels;
import com.example.tgpublisher.service.TelegramClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublishController.class)
class PublishControllerTextTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelegramClient telegramClient;

    @Test
    void publishTextSuccess() throws Exception {
        TelegramApiModels.Message message = new TelegramApiModels.Message();
        message.setMessageId(123L);
        TelegramApiModels.SendMessageResponse response = new TelegramApiModels.SendMessageResponse();
        response.setOk(true);
        response.setResult(message);
        when(telegramClient.sendMessage("hello")).thenReturn(response);

        mockMvc.perform(post("/publish/text")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.telegramMessageId").value(123));

        verify(telegramClient).sendMessage("hello");
    }

    @Test
    void publishTextTooLong() throws Exception {
        String longText = "a".repeat(4097);

        mockMvc.perform(post("/publish/text")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(longText))
                .andExpect(status().isBadRequest());

        verify(telegramClient, never()).sendMessage(anyString());
    }
}
