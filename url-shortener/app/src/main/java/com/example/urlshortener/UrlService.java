package com.example.urlshortener;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UrlService {

  private final UrlRepository repo;
  private final UrlCache cache;

  public UrlService(UrlRepository repo, UrlCache cache) {
    this.repo = repo;
    this.cache = cache;
  }

  public String shorten(String longUrl) {
    long id = repo.nextId();
    String shortKey = Base62.encode(id);
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
