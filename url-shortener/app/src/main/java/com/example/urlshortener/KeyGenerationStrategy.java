package com.example.urlshortener;

/**
 * Produces a short key for a newly allocated record. Each strategy may choose to use the
 * pre-allocated sequence id, the long URL, or both. Collision-prone strategies are responsible for
 * ensuring the returned key is unique.
 */
public interface KeyGenerationStrategy {
  String generate(long id, String longUrl);
}
