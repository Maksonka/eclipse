package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Services.E2eKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/e2e")
public class E2eKeyController {

    private final E2eKeyService e2eKeyService;

    public E2eKeyController(E2eKeyService e2eKeyService) {
        this.e2eKeyService = e2eKeyService;
    }

    @PostMapping("/key")
    public ResponseEntity<?> uploadKey(@RequestBody Map<String, String> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        String publicKey = body == null ? null : body.get("publicKey");
        if (publicKey == null || publicKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "publicKey обязателен"));
        }
        if (!e2eKeyService.saveIdentityKey(principal.getName(), publicKey)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Некорректный ключ"));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/key/{username}")
    public ResponseEntity<?> getKey(@PathVariable String username, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        if (username.equals(principal.getName())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Нельзя получить свой ключ так"));
        }
        return e2eKeyService.findIdentityPublicKey(username)
                .map(pub -> ResponseEntity.ok(Map.of("username", username, "publicKey", pub)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "У собеседника нет ключа E2E")));
    }
}
