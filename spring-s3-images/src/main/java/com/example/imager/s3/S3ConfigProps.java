package com.example.imager.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public class S3ConfigProps {
    private String endpoint;        // обязателен (e.g. http://127.0.0.1:9000)
    private String region;          // обязателен (e.g. us-east-1)
    private String bucket;          // обязателен (e.g. images)
    private String accessKey;       // обязателен (или используйте иную стратегию кредов)
    private String secretKey;       // обязателен
    private boolean pathStyleAccess = true; // можно оставить дефолт, чаще нужен для MinIO
    private long maxSize = 10 * 1024 * 1024; // лимит размера, опционально

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public boolean isPathStyleAccess() { return pathStyleAccess; }
    public void setPathStyleAccess(boolean pathStyleAccess) { this.pathStyleAccess = pathStyleAccess; }
    public long getMaxSize() { return maxSize; }
    public void setMaxSize(long maxSize) { this.maxSize = maxSize; }
}
