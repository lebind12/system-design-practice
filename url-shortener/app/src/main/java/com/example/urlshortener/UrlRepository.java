package com.example.urlshortener;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UrlRepository {

  private final JdbcTemplate jdbc;

  public UrlRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long nextId() {
    Long id = jdbc.queryForObject("SELECT nextval('url_id_seq')", Long.class);
    if (id == null) {
      throw new IllegalStateException("nextval returned null");
    }
    return id;
  }

  public void insert(UrlEntry entry) {
    jdbc.update(
        "INSERT INTO urls (id, short_key, long_url) VALUES (?, ?, ?)",
        entry.id(),
        entry.shortKey(),
        entry.longUrl());
  }

  public Optional<String> findLongUrl(String shortKey) {
    return jdbc
        .query(
            "SELECT long_url FROM urls WHERE short_key = ?",
            ps -> ps.setString(1, shortKey),
            rs -> rs.next() ? Optional.of(rs.getString(1)) : Optional.<String>empty());
  }

  public boolean existsByShortKey(String shortKey) {
    Boolean exists =
        jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM urls WHERE short_key = ?)",
            Boolean.class,
            shortKey);
    return Boolean.TRUE.equals(exists);
  }
}
