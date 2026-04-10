package com.example.scaling.lab1;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @Value("${app.instance-id:default}")
  private String instanceId;

  @Value("${app.storage-mode:memory}")
  private String storageMode;

  @Value("${app.cache-enabled:false}")
  private boolean cacheEnabled;

  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of(
        "status", "ok",
        "instance", instanceId,
        "storage", storageMode,
        "cache", cacheEnabled);
  }
}
