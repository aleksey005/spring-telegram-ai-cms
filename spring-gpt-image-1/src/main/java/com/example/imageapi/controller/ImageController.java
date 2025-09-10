package com.example.imageapi.controller;

import com.example.imageapi.dto.ImageRequest;
import com.example.imageapi.dto.ImageResponse;
import com.example.imageapi.service.OpenAIImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {
  private final OpenAIImageService service;

  /**
   * Возвращает JSON с массивом строк: base64 или URL, в зависимости от флага b64.
   */
  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ImageResponse generateJson(@Valid @RequestBody ImageRequest req) throws Exception {
    return service.generate(req);
  }

  /**
   * Сразу отдает PNG, беря первый элемент из base64-множества.
   * Удобно для <img src> на фронте.
   */
  @PostMapping(value = "/png", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> generatePng(@Valid @RequestBody ImageRequest req) throws Exception {
    req.setB64(true); // принудительно base64
    ImageResponse resp = service.generate(req);
    if (resp.getData().isEmpty()) return ResponseEntity.noContent().build();
    byte[] png = Base64.getDecoder().decode(resp.getData().get(0));
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .contentType(MediaType.IMAGE_PNG)
        .body(png);
  }
}