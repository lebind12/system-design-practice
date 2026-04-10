package com.example.urlshortener;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UrlController {

  private final UrlService service;

  public UrlController(UrlService service) {
    this.service = service;
  }

  public record ShortenRequest(@NotBlank @Size(max = 2048) String longUrl) {}

  public record ShortenResponse(String shortKey, String shortUrl) {}

  @PostMapping("/shorten")
  public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest req) {
    String shortKey = service.shorten(req.longUrl());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ShortenResponse(shortKey, "/" + shortKey));
  }

  @GetMapping("/{shortKey:[0-9A-Za-z]{1,16}}")
  public ResponseEntity<Void> redirect(@PathVariable String shortKey) {
    Optional<String> longUrl = service.resolve(shortKey);
    if (longUrl.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(longUrl.get())).build();
  }
}
