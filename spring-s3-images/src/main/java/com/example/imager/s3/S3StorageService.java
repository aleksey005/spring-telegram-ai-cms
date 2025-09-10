package com.example.imager.s3;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class S3StorageService {

    private final S3Client s3;
    private final S3ConfigProps props;
    private final Tika tika = new Tika();

    public S3StorageService(S3Client s3, S3ConfigProps props) {
        this.s3 = s3;
        this.props = props;
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(props.getBucket()).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                s3.createBucket(CreateBucketRequest.builder().bucket(props.getBucket()).build());
            }
        }
    }

    public UploadResult storeDataUrl(String messageId, String dataUrl) {
        int comma = dataUrl.indexOf(',');
        if (comma < 0) throw new IllegalArgumentException("Invalid data URL");

        String meta = dataUrl.substring(0, comma);
        String b64  = dataUrl.substring(comma + 1);

        if (!meta.contains(";base64")) {
            throw new IllegalArgumentException("Only base64 data URLs are supported");
        }

        String contentType = "application/octet-stream";
        int c = meta.indexOf(':');
        int sc = meta.indexOf(';');
        if (c >= 0 && sc > c) {
            contentType = meta.substring(c + 1, sc);
        }

        byte[] bytes = Base64.getDecoder().decode(b64.getBytes(StandardCharsets.US_ASCII));

        if (bytes.length == 0) throw new IllegalArgumentException("Empty image");
        if (bytes.length > props.getMaxSize()) throw new IllegalArgumentException("Image too large");

        String detected = tika.detect(bytes);
        if (detected != null && !detected.equals("application/octet-stream")) {
            contentType = detected;
        }

    String ext;
    switch (contentType) {
        case "image/png":
            ext = "png";
            break;
        case "image/jpeg":
            ext = "jpg";
            break;
        case "image/webp":
            ext = "webp";
            break;
        case "image/gif":
            ext = "gif";
            break;
        default:
            ext = "bin";
            break;
    }


        String sanitizedId = sanitize(messageId);
        String key = sanitizedId + "." + ext;

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(contentType)
                .acl(ObjectCannedACL.PRIVATE)
                .build();

        s3.putObject(put, RequestBody.fromBytes(bytes));

        return new UploadResult(key, contentType, bytes.length);
    }

    public ResponseBytes<GetObjectResponse> fetch(String messageId) {
        String prefix = sanitize(messageId) + ".";
        ListObjectsV2Response listed = s3.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(props.getBucket())
                .prefix(prefix)
                .maxKeys(1)
                .build());

        if (listed.contents().isEmpty()) {
            throw NoSuchKeyException.builder().message("Not found").build();
        }
        String key = listed.contents().get(0).key();

        return s3.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build());
    }

    private String sanitize(String s) {
        String trimmed = s.trim();
        if (!StringUtils.hasText(trimmed)) throw new IllegalArgumentException("message_id is blank");
        String safe = trimmed.replaceAll("[^A-Za-z0-9_\\-]", "_");
        if (safe.length() > 200) safe = safe.substring(0, 200);
        return safe;
    }
}
