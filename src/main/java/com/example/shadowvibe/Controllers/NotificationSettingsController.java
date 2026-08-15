package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Services.GroupService;
import com.example.shadowvibe.Services.MuteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationSettingsController {

    private final MuteService muteService;
    private final GroupService groupService;

    public NotificationSettingsController(MuteService muteService, GroupService groupService) {
        this.muteService = muteService;
        this.groupService = groupService;
    }

    @GetMapping("/muted-chats")
    public Map<String, Object> mutedChats(Principal principal) {
        return Map.of(
                "direct", muteService.getMutedDirectPartners(principal.getName()),
                "groups", muteService.getMutedGroupIds(principal.getName())
        );
    }

    @PostMapping("/direct")
    public ResponseEntity<Map<String, Object>> setDirectMuted(Principal principal,
                                                              @RequestBody Map<String, String> body) {
        String partnerUsername = body.get("partnerUsername");
        boolean muted = Boolean.parseBoolean(body.getOrDefault("muted", "false"));
        try {
            boolean result = muteService.setDirectMuted(principal.getName(), partnerUsername, muted);
            return ResponseEntity.ok(Map.of("muted", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/group")
    public ResponseEntity<Map<String, Object>> setGroupMuted(Principal principal,
                                                             @RequestBody Map<String, String> body) {
        Long groupId;
        try {
            groupId = Long.valueOf(body.get("groupId"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Некорректный id группы"));
        }
        boolean muted = Boolean.parseBoolean(body.getOrDefault("muted", "false"));
        try {
            groupService.getGroupForMember(groupId, principal.getName());
            boolean result = muteService.setGroupMuted(principal.getName(), groupId, muted);
            return ResponseEntity.ok(Map.of("muted", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
