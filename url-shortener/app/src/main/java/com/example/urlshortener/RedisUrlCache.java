package com.example.urlshortener;

import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed implementation activated by {@code app.cache-enabled=true}. Uses write-through on
 * shorten so the first redirect never pays a miss, which matches "once the URL is public, every
 * click is hot" access pattern.
 */
@Component
@ConditionalOnProperty(name = "app.cache-enabled", havingValue = "true")
public class RedisUrlCache implements UrlCache {

  private static final String PREFIX = "u:";
  private static final Duration TTL = Duration.ofHours(24);

  private final StringRedisTemplate redis;

  public RedisUrlCache(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Optional<String> get(String shortKey) {
    return Optional.ofNullable(redis.opsForValue().get(PREFIX + shortKey));
  }

  @Override
  public void put(String shortKey, String longUrl) {
    redis.opsForValue().set(PREFIX + shortKey, longUrl, TTL);
  }
}
