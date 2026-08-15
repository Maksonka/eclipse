package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.DTO.ScheduleMessageRequest;
import com.example.shadowvibe.DTO.ScheduledMessageDto;
import com.example.shadowvibe.Services.ScheduledMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduled")
public class ScheduledMessageController {

    private final ScheduledMessageService scheduledMessageService;

    public ScheduledMessageController(ScheduledMessageService scheduledMessageService) {
        this.scheduledMessageService = scheduledMessageService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ScheduleMessageRequest request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        try {
            ScheduledMessageDto dto = scheduledMessageService.schedule(principal.getName(), request);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<ScheduledMessageDto> list(Principal principal) {
        if (principal == null) {
            return List.of();
        }
        return scheduledMessageService.list(principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        boolean cancelled = scheduledMessageService.cancel(id, principal.getName());
        if (!cancelled) {
            return ResponseEntity.status(404).body(Map.of("error", "Отложенное сообщение не найдено"));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
