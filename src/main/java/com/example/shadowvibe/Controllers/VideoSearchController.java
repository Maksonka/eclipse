package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Services.search.VideoSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
public class VideoSearchController {

    private final VideoSearchService videoSearchService;

    public VideoSearchController(VideoSearchService videoSearchService) {
        this.videoSearchService = videoSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "12") int limit,
                                    Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Введите запрос"));
        }
        String query = q.trim();
        if (query.length() > 200) {
            query = query.substring(0, 200);
        }
        if (limit < 1) {
            limit = 1;
        }
        if (limit > 20) {
            limit = 20;
        }
        List<?> results = videoSearchService.search(query, limit);
        return ResponseEntity.ok(Map.of("query", query, "results", results));
    }
}
