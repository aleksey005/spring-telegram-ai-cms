package com.example.suggest;

import java.io.IOException;
import java.util.List;

public interface SuggestService {
  List<SuggestionItem> suggest(String q, String lang, int limit) throws IOException;
}
