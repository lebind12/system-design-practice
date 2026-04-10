package com.example.urlshortener;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default cache bean used when {@code app.cache-enabled} is false or absent. Keeps the MVP read
 * path pinned at "single DB query per lookup" so the baseline measurement stays clean.
 */
@Component
@ConditionalOnProperty(name = "app.cache-enabled", havingValue = "false", matchIfMissing = true)
public class NoopUrlCache implements UrlCache {

  @Override
  public Optional<String> get(String shortKey) {
    return Optional.empty();
  }

  @Override
  public void put(String shortKey, String longUrl) {
    // no-op
  }
}
