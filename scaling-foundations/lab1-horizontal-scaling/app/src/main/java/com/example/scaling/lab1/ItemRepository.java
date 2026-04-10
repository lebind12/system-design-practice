package com.example.scaling.lab1;

import java.util.Optional;

public interface ItemRepository {
  Optional<Item> findById(String id);
  void save(Item item);
}
