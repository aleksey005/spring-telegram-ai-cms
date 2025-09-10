package com.example.telegramclient.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalIngestPublisherTest {

    @Test
    void sendsMessageIdAsTitle() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200));
        server.start();
        try {
            ExternalIngestPublisher publisher = new ExternalIngestPublisher(
                    true,
                    server.url("/ingest").toString(),
                    1000
            );
            publisher.ingestText("hello", 42L);

            var recorded = server.takeRequest(5, TimeUnit.SECONDS);
            assertThat(recorded).isNotNull();
            String body = recorded.getBody().readUtf8();
            Map<?, ?> map = new ObjectMapper().readValue(body, Map.class);
            assertThat(map.get("title")).isEqualTo("42");
            assertThat(map.get("text")).isEqualTo("hello");
        } finally {
            server.shutdown();
        }
    }
}

