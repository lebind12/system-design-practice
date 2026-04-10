package com.example.scaling.lab1;

import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

  private static final Duration TTL = Duration.ofSeconds(60);

  private final ItemRepository repo;
  private final StringRedisTemplate redis;
  private final boolean cacheEnabled;

  public ItemService(ItemRepository repo,
                     @Value("${app.cache-enabled:false}") boolean cacheEnabled,
                     ObjectProvider<StringRedisTemplate> redisProvider) {
    this.repo = repo;
    this.cacheEnabled = cacheEnabled;
    this.redis = cacheEnabled ? redisProvider.getIfAvailable() : null;
  }

  public Optional<Item> get(String id) {
    if (redis != null) {
      String cached = redis.opsForValue().get(key(id));
      if (cached != null) {
        return Optional.of(deserialize(cached));
      }
      Optional<Item> result = repo.findById(id);
      result.ifPresent(item -> redis.opsForValue().set(key(id), serialize(item), TTL));
      return result;
    }
    return repo.findById(id);
  }

  public void save(Item item) {
    repo.save(item);
    if (redis != null) {
      redis.delete(key(item.id()));
    }
  }

  private static String key(String id) {
    return "item:" + id;
  }

  private static String serialize(Item item) {
    return item.id() + "|" + item.name() + "|" + item.views();
  }

  private static Item deserialize(String s) {
    String[] parts = s.split("\\|", 3);
    return new Item(parts[0], parts[1], Long.parseLong(parts[2]));
  }
}
