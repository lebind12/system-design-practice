package com.example.scaling.lab1;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.storage-mode", havingValue = "jdbc")
public class JdbcItemRepository implements ItemRepository {

  private final JdbcTemplate jdbc;

  public JdbcItemRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<Item> findById(String id) {
    try {
      Item item = jdbc.queryForObject(
          "SELECT id, name, views FROM items WHERE id = ?",
          (rs, i) -> new Item(rs.getString("id"), rs.getString("name"), rs.getLong("views")),
          id);
      return Optional.ofNullable(item);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public void save(Item item) {
    jdbc.update(
        "INSERT INTO items(id, name, views) VALUES (?, ?, ?) " +
        "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name",
        item.id(), item.name(), item.views());
  }
}
