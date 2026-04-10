package com.example.scaling.lab1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
public class ItemController {

  private final ItemService service;

  public ItemController(ItemService service) {
    this.service = service;
  }

  @GetMapping("/{id}")
  public ResponseEntity<Item> get(@PathVariable String id) {
    return service.get(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<Item> create(@RequestBody CreateRequest req) {
    Item item = new Item(req.id(), req.name(), 0);
    service.save(item);
    return ResponseEntity.ok(item);
  }

  public record CreateRequest(String id, String name) {}
}
