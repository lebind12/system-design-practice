package com.example.urlshortener;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @Value("${app.cache-enabled:false}")
  private boolean cacheEnabled;

  @Value("${app.key-strategy:base62}")
  private String keyStrategy;

  @Value("${app.dedup-mode:off}")
  private String dedupMode;

  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of(
        "status", "ok",
        "cacheEnabled", cacheEnabled,
        "keyStrategy", keyStrategy,
        "dedupMode", dedupMode);
  }
}
