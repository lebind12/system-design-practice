package com.example.urlshortener;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default strategy: encode the pre-allocated sequence id with Base62. No collision check is needed
 * because the id is globally unique (comes from a DB sequence). Single DB round trip (the
 * {@code nextval} is already consumed by UrlService before this runs).
 */
@Component
@ConditionalOnProperty(name = "app.key-strategy", havingValue = "base62", matchIfMissing = true)
public class Base62Strategy implements KeyGenerationStrategy {

  @Override
  public String generate(long id, String longUrl) {
    return Base62.encode(id);
  }
}
