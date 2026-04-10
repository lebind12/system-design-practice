package com.example.urlshortener;

import java.util.Optional;

/**
 * Optional pre-insert lookup: if the same longUrl was already shortened, reuse the existing key
 * instead of allocating a new one. Active strategy is chosen by {@code app.dedup-mode}.
 */
public interface DedupStrategy {
  Optional<String> findExistingShortKey(String longUrl);
}
