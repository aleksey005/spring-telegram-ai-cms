package com.example.imager.s3;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(S3ConfigProps.class) // регистрируем бин с пропертями
public class S3Config {

    @Bean
    public S3Client s3Client(S3ConfigProps props) {
        // Жёсткая валидация обязательных параметров
        requireNotBlank(props.getEndpoint(), "app.s3.endpoint");
        requireNotBlank(props.getRegion(),   "app.s3.region");
        requireNotBlank(props.getBucket(),   "app.s3.bucket");
        requireNotBlank(props.getAccessKey(), "app.s3.accessKey");
        requireNotBlank(props.getSecretKey(), "app.s3.secretKey");

        URI endpoint = URI.create(props.getEndpoint());
        if (endpoint.getScheme() == null) {
            throw new IllegalArgumentException("app.s3.endpoint должен включать схему, например http://127.0.0.1:9000");
        }

        var credentials = AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey());
        var s3cfg = S3Configuration.builder()
                .pathStyleAccessEnabled(props.isPathStyleAccess())
                .build();

        return S3Client.builder()
                .httpClient(UrlConnectionHttpClient.builder().build())
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(props.getRegion()))
                .endpointOverride(endpoint)
                .serviceConfiguration(s3cfg)
                .build();
    }

    private static void requireNotBlank(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Отсутствует обязательная настройка: " + name);
        }
    }
}
