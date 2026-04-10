package com.example.urlshortener;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Default: never reuse. Every shorten call produces a new record. */
@Component
@ConditionalOnProperty(name = "app.dedup-mode", havingValue = "off", matchIfMissing = true)
public class NoopDedupStrategy implements DedupStrategy {

  @Override
  public Optional<String> findExistingShortKey(String longUrl) {
    return Optional.empty();
  }
}
