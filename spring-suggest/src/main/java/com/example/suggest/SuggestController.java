package com.example.suggest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/suggest")
public class SuggestController {

  private final SuggestService service;

  public SuggestController(SuggestService service) {
    this.service = service;
  }

  @GetMapping
  public Map<String, Object> suggest(
      @RequestParam("q") String q,
      @RequestParam(value = "lang", defaultValue = "ru") String lang,
      @RequestParam(value = "loc", defaultValue = "") String loc,
      @RequestParam(value = "limit", defaultValue = "8") int limit,
      @RequestHeader("X-Session") String session,
      @RequestHeader(value = "X-User-Id", required = false) String userId)
      throws IOException {
    long start = System.currentTimeMillis();
    List<SuggestionItem> items = service.suggest(q, lang, limit);
    long latency = System.currentTimeMillis() - start;

    Map<String, Object> resp = new HashMap<>();
    resp.put("q", q);
    resp.put("suggestions", items);
    resp.put("latency_ms", latency);
    return resp;
  }
}
