package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Services.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        List<?> favorites = favoriteService.getFavorites(principal.getName());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("favorites", favorites);
        response.put("count", favorites.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> count(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        return ResponseEntity.ok(Map.of("count", favoriteService.count(principal.getName())));
    }

    @GetMapping("/ids")
    public ResponseEntity<Map<String, Object>> ids(@RequestParam(defaultValue = "DIRECT") String type,
                                                   @RequestParam(required = false) String partner,
                                                   @RequestParam(required = false) Long groupId,
                                                   Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        List<Long> ids = favoriteService.getFavoritedIds(principal.getName(), type, partner, groupId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ids", ids);
        return ResponseEntity.ok(response);
    }
}
