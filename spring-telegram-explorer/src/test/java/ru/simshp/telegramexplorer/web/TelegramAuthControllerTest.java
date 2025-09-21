package ru.simshp.telegramexplorer.web;

import dev.voroby.springframework.telegram.client.TelegramClient;
import dev.voroby.springframework.telegram.client.templates.response.Response;
import org.drinkless.tdlib.TdApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelegramAuthController.class)
class TelegramAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelegramClient telegramClient;

    @Test
    void submitCodeReturnsOkWhenTdLibAcceptsCode() throws Exception {
        @SuppressWarnings("unchecked")
        Response<TdApi.Ok> response = mock(Response.class);
        given(response.getError()).willReturn(Optional.empty());
        given(telegramClient.send(any(TdApi.CheckAuthenticationCode.class))).willReturn(response);

        mockMvc.perform(post("/telegram/auth/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"12345\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<TdApi.CheckAuthenticationCode> captor =
                ArgumentCaptor.forClass(TdApi.CheckAuthenticationCode.class);
        verify(telegramClient).send(captor.capture());
        assertThat(captor.getValue().code).isEqualTo("12345");
    }

    @Test
    void submitCodeReturnsBadRequestWhenTdLibReturnsError() throws Exception {
        TdApi.Error error = new TdApi.Error();
        error.code = 400;
        error.message = "Invalid authentication code";

        @SuppressWarnings("unchecked")
        Response<TdApi.Ok> response = mock(Response.class);
        given(response.getError()).willReturn(Optional.of(error));
        given(telegramClient.send(any(TdApi.CheckAuthenticationCode.class))).willReturn(response);

        mockMvc.perform(post("/telegram/auth/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to verify authentication code"))
                .andExpect(jsonPath("$.details").value("Invalid authentication code"));
    }
}
