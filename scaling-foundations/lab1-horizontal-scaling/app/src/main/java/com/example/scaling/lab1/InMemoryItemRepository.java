package com.example.scaling.lab1;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.storage-mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryItemRepository implements ItemRepository {

  private final ConcurrentHashMap<String, Item> store = new ConcurrentHashMap<>();

  @Override
  public Optional<Item> findById(String id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public void save(Item item) {
    store.put(item.id(), item);
  }
}
