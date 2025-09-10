// /src/main/java/ru/simshp/qdrant/config/WebClientConfig.java
package ru.simshp.qdrant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean("qdrantWebClient")
    public WebClient qdrantWebClient(AppProperties props) {
        WebClient.Builder b = WebClient.builder()
                .baseUrl(props.getQdrant().getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()));
        if (props.getQdrant().getApiKey() != null && !props.getQdrant().getApiKey().isBlank()) {
            b.defaultHeader("api-key", props.getQdrant().getApiKey());
        }
        return b.build();
    }

    @Bean("openaiWebClient")
    public WebClient openaiWebClient(AppProperties props) {
        return WebClient.builder()
                .baseUrl(props.getEmbedding().getOpenai().getApiUrl())
                .defaultHeader("Authorization", "Bearer " + props.getEmbedding().getOpenai().getApiKey())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .build();
    }
}
