package com.example.imageapi.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageResponse {
  /** "b64" или "url" */
  private String type;
  private List<String> data;
}