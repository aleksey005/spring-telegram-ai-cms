package com.example.suggest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.collapse.CollapseBuilder;
import org.opensearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SuggestServiceImpl implements SuggestService {

  private final RestHighLevelClient client;
  private final String index;

  public SuggestServiceImpl(
      RestHighLevelClient client, @Value("${suggest.index:suggestions}") String index) {
    this.client = client;
    this.index = index;
  }

  @Override
  public List<SuggestionItem> suggest(String q, String lang, int limit) throws IOException {
    MultiMatchQueryBuilder mqb =
        QueryBuilders.multiMatchQuery(q)
            .type(MultiMatchQueryBuilder.Type.BOOL_PREFIX)
            .field("term")
            .field("term._2gram")
            .field("term._3gram")
            .field("term._index_prefix");

    SearchSourceBuilder ssb =
        new SearchSourceBuilder()
            .query(mqb)
            .sort("freq", SortOrder.DESC)
            .collapse(new CollapseBuilder("term_kw"))
            .fetchField("term_kw")
            .size(limit);

    SearchRequest req = new SearchRequest(index).source(ssb);
    SearchResponse resp = client.search(req, RequestOptions.DEFAULT);

    return Arrays.stream(resp.getHits().getHits())
        .map(hit -> hit.getFields().get("term_kw").getValue().toString())
        .map(t -> new SuggestionItem(t, "prefix"))
        .toList();
  }
}
