package com.example.imageapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageRequest {
  @NotBlank
  private String prompt;

  /** Размер, допустим "1024x1024". */
  private String size = "1024x1024";

  /** Количество изображений. */
  @Min(1)
  private int n = 1;

  /** Возврат как base64 (true) или URL (false). */
  private boolean b64 = true;

  /** Модель: по умолчанию используем dall-e-3 (не требует Verify Organization). */
  private String model = "dall-e-3";
}