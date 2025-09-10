package com.example.imager.s3;

public class UploadResult {
    private final String key;
    private final String contentType;
    private final long size;

    public UploadResult(String key, String contentType, long size) {
        this.key = key;
        this.contentType = contentType;
        this.size = size;
    }

    public String getKey() {
        return key;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }
}
