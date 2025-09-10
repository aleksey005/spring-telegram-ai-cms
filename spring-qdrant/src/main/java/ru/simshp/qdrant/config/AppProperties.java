// /src/main/java/ru/simshp/qdrant/config/AppProperties.java
package ru.simshp.qdrant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Qdrant qdrant = new Qdrant();
    private Embedding embedding = new Embedding();

    public Qdrant getQdrant() { return qdrant; }
    public Embedding getEmbedding() { return embedding; }

    public static class Qdrant {
        private String baseUrl;
        private String apiKey;
        private String collection;
        private int vectorSize;
        private String distance;
        private boolean createIfMissing;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getCollection() { return collection; }
        public void setCollection(String collection) { this.collection = collection; }
        public int getVectorSize() { return vectorSize; }
        public void setVectorSize(int vectorSize) { this.vectorSize = vectorSize; }
        public String getDistance() { return distance; }
        public void setDistance(String distance) { this.distance = distance; }
        public boolean isCreateIfMissing() { return createIfMissing; }
        public void setCreateIfMissing(boolean createIfMissing) { this.createIfMissing = createIfMissing; }
    }

    public static class Embedding {
        private String provider;
        private OpenAI openai = new OpenAI();

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public OpenAI getOpenai() { return openai; }

        public static class OpenAI {
            private String apiUrl;
            private String apiKey;
            private String model;

            public String getApiUrl() { return apiUrl; }
            public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getModel() { return model; }
            public void setModel(String model) { this.model = model; }
        }
    }
}
