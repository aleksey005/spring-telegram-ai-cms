package com.example.suggest;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

  @Bean
  RestHighLevelClient restHighLevelClient(
      @Value("${opensearch.host:localhost}") String host,
      @Value("${opensearch.port:9200}") int port,
      @Value("${opensearch.scheme:http}") String scheme,
      @Value("${opensearch.pathPrefix:}") String pathPrefix,
      @Value("${opensearch.username:}") String username,
      @Value("${opensearch.password:}") String password) {
    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(
        AuthScope.ANY, new UsernamePasswordCredentials(username, password));
    RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme));
    if (!pathPrefix.isEmpty()) {
      builder.setPathPrefix(pathPrefix);
    }
    builder.setHttpClientConfigCallback(
        httpClientBuilder ->
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
    return new RestHighLevelClient(builder);
  }
}
