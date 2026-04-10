package com.example.urlshortener;

import java.util.Optional;

public interface UrlCache {
  Optional<String> get(String shortKey);

  void put(String shortKey, String longUrl);
}
