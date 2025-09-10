package com.example.imager.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadRequest {
    @NotBlank
    private String message_id;

    @NotBlank
    private String image_base64;
}
