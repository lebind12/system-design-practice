package com.example.urlshortener;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DB-backed dedup via a functional {@code md5(long_url)} index. Executes one extra SELECT per
 * shorten to check whether the URL was already shortened; on hit, reuses the existing key and
 * skips the INSERT entirely. Worst case (always unique URLs) is one extra index lookup per write;
 * best case (always duplicates) turns shortens into reads.
 */
@Component
@ConditionalOnProperty(name = "app.dedup-mode", havingValue = "db")
public class DbDedupStrategy implements DedupStrategy {

  private final UrlRepository repo;

  public DbDedupStrategy(UrlRepository repo) {
    this.repo = repo;
  }

  @Override
  public Optional<String> findExistingShortKey(String longUrl) {
    return repo.findShortKeyByLongUrl(longUrl);
  }
}
