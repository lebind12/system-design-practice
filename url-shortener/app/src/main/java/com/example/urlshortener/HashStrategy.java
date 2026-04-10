package com.example.urlshortener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Hash + collision resolution strategy from the book. Take the first {@link #KEY_LEN} hex chars of
 * {@code md5(longUrl || salt)} and probe the DB for uniqueness. On collision, bump the salt and
 * retry. This costs an extra DB round trip per shorten (the {@code existsByShortKey} lookup),
 * which is exactly the write-path overhead the experiment aims to measure.
 */
@Component
@ConditionalOnProperty(name = "app.key-strategy", havingValue = "hash")
public class HashStrategy implements KeyGenerationStrategy {

  private static final int KEY_LEN = 7;
  private static final int MAX_RETRIES = 5;

  private final UrlRepository repo;

  public HashStrategy(UrlRepository repo) {
    this.repo = repo;
  }

  @Override
  public String generate(long id, String longUrl) {
    String salt = "";
    for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      String candidate = md5Hex(longUrl + salt).substring(0, KEY_LEN);
      if (!repo.existsByShortKey(candidate)) {
        return candidate;
      }
      salt = "::retry-" + attempt;
    }
    throw new IllegalStateException(
        "HashStrategy exhausted " + MAX_RETRIES + " retries for " + longUrl);
  }

  private static String md5Hex(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 missing", e);
    }
  }
}
