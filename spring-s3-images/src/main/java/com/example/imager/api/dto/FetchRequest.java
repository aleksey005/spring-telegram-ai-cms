package com.example.imager.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FetchRequest {
    @NotBlank
    private String message_id;
}
