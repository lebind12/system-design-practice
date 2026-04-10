package com.example.urlshortener;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UrlService {

  private final UrlRepository repo;

  public UrlService(UrlRepository repo) {
    this.repo = repo;
  }

  public String shorten(String longUrl) {
    long id = repo.nextId();
    String shortKey = Base62.encode(id);
    repo.insert(new UrlEntry(id, shortKey, longUrl));
    return shortKey;
  }

  public Optional<String> resolve(String shortKey) {
    return repo.findLongUrl(shortKey);
  }
}
