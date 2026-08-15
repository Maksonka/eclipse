package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.DTO.MessageSearchResultDto;
import com.example.shadowvibe.Services.GroupService;
import com.example.shadowvibe.Services.MessageSearchService;
import com.example.shadowvibe.Services.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class MessageSearchController {

    private final MessageSearchService messageSearchService;
    private final MessageService messageService;
    private final GroupService groupService;

    public MessageSearchController(MessageSearchService messageSearchService,
                                   MessageService messageService,
                                   GroupService groupService) {
        this.messageSearchService = messageSearchService;
        this.messageService = messageService;
        this.groupService = groupService;
    }

    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> search(@RequestParam(defaultValue = "") String q,
                                                      @RequestParam(defaultValue = "all") String scope,
                                                      @RequestParam(required = false) String partner,
                                                      @RequestParam(required = false) Long groupId,
                                                      @RequestParam(defaultValue = "50") int limit,
                                                      Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        if (q == null || q.trim().isBlank()) {
            return ResponseEntity.ok(Map.of("results", List.of(), "count", 0));
        }
        if (limit < 1) {
            limit = 1;
        }
        if (limit > 100) {
            limit = 100;
        }

        String query = q.trim();
        List<MessageSearchResultDto> results;

        if ("direct".equalsIgnoreCase(scope)) {
            if (partner == null || partner.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Укажите partner для поиска в диалоге"));
            }
            results = messageService.searchDirectHistory(principal.getName(), partner.trim(), query, limit);
        } else if ("group".equalsIgnoreCase(scope)) {
            if (groupId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Укажите groupId для поиска в группе"));
            }
            results = groupService.searchGroupHistory(groupId, principal.getName(), query, limit);
        } else {
            results = messageSearchService.searchAll(principal.getName(), query, limit);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        response.put("count", results.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/messages/{id}/context")
    public ResponseEntity<Map<String, Object>> context(@PathVariable long id,
                                                       @RequestParam(defaultValue = "direct") String type,
                                                       @RequestParam(required = false) String partner,
                                                       @RequestParam(required = false) Long groupId,
                                                       @RequestParam(defaultValue = "60") int size,
                                                       Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        if (size < 10) {
            size = 10;
        }
        if (size > 100) {
            size = 100;
        }

        List<?> messages;
        if ("group".equalsIgnoreCase(type)) {
            if (groupId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Укажите groupId"));
            }
            try {
                messages = groupService.getGroupWindowEndingAt(principal.getName(), groupId, id, size);
            } catch (RuntimeException e) {
                return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
            }
        } else {
            if (partner == null || partner.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Укажите partner"));
            }
            messages = messageService.getChatWindowEndingAt(principal.getName(), partner.trim(), id, size);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("messages", messages);
        response.put("anchorId", id);
        return ResponseEntity.ok(response);
    }
}
