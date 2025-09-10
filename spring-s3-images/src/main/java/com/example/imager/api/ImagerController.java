package com.example.imager.api;

import com.example.imager.api.dto.FetchRequest;
import com.example.imager.api.dto.UploadRequest;
import com.example.imager.s3.S3StorageService;
import com.example.imager.s3.UploadResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class ImagerController {

    private final S3StorageService storage;

    public ImagerController(S3StorageService storage) {
        this.storage = storage;
    }

    @PostMapping(
            path = "/upload",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> upload(@Valid @RequestBody UploadRequest req) {
        UploadResult result = storage.storeDataUrl(req.getMessage_id(), req.getImage_base64());

        String json = "{ \"status\": \"ok\", " +
                "\"key\": \"" + result.getKey() + "\"," +
                "\"content_type\": \"" + result.getContentType() + "\"," +
                "\"size\": " + result.getSize() + " }";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    // ImagerController.java (метод fetch)
    @PostMapping(path = "/fetch", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> fetch(@Valid @RequestBody FetchRequest req) {
        var bytes = storage.fetch(req.getMessage_id());
        String ct = bytes.response().contentType();
        if (ct == null || ct.isBlank()) ct = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        // имя файла по message_id + расширение из Content-Type
        String ext = switch (ct) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "bin";
        };
        String filename = req.getMessage_id().replaceAll("[^A-Za-z0-9_\\-]", "_") + "." + ext;

        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(ct))
            .body(bytes.asByteArray());
    }

    @GetMapping("/healthz")
    public String health() {
        return "OK";
    }
}
