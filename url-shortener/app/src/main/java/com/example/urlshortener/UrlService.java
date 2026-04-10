package com.example.urlshortener;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UrlService {

  private final UrlRepository repo;
  private final UrlCache cache;
  private final KeyGenerationStrategy keyStrategy;
  private final DedupStrategy dedupStrategy;

  public UrlService(
      UrlRepository repo,
      UrlCache cache,
      KeyGenerationStrategy keyStrategy,
      DedupStrategy dedupStrategy) {
    this.repo = repo;
    this.cache = cache;
    this.keyStrategy = keyStrategy;
    this.dedupStrategy = dedupStrategy;
  }

  public String shorten(String longUrl) {
    Optional<String> existing = dedupStrategy.findExistingShortKey(longUrl);
    if (existing.isPresent()) {
      return existing.get();
    }
    long id = repo.nextId();
    String shortKey = keyStrategy.generate(id, longUrl);
    repo.insert(new UrlEntry(id, shortKey, longUrl));
    cache.put(shortKey, longUrl);
    return shortKey;
  }

  public Optional<String> resolve(String shortKey) {
    Optional<String> cached = cache.get(shortKey);
    if (cached.isPresent()) {
      return cached;
    }
    Optional<String> fromDb = repo.findLongUrl(shortKey);
    fromDb.ifPresent(url -> cache.put(shortKey, url));
    return fromDb;
  }
}
