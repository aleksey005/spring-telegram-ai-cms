package com.example.suggest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SuggestController.class)
class SuggestControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private SuggestService service;

  @Test
  void returnsSuggestions() throws Exception {
    List<SuggestionItem> items =
        List.of(
            new SuggestionItem("kubernetes", "popular"),
            new SuggestionItem("kubernetes deployment", "prefix"));
    when(service.suggest("kuber", "ru", 2)).thenReturn(items);

    mockMvc
        .perform(get("/suggest").param("q", "kuber").param("limit", "2").header("X-Session", "s1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.q").value("kuber"))
        .andExpect(jsonPath("$.suggestions[0].text").value("kubernetes"))
        .andExpect(jsonPath("$.suggestions[0].reason").value("popular"))
        .andExpect(jsonPath("$.latency_ms").isNumber());
  }
}
