package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.GroupInvite;
import com.example.shadowvibe.Services.GroupInviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group-invites")
public class GroupInviteController {

    private final GroupInviteService inviteService;

    public GroupInviteController(GroupInviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        List<GroupInvite> invites = inviteService.getPendingInvites(principal.getName());
        List<Map<String, Object>> items = invites.stream().map(this::toDto).toList();
        return ResponseEntity.ok(Map.of("invites", items, "count", items.size()));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> count(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        return ResponseEntity.ok(Map.of("count", inviteService.getPendingCount(principal.getName())));
    }

    @PostMapping("/{groupId}/create")
    public ResponseEntity<Map<String, Object>> create(@PathVariable Long groupId,
                                                      @RequestBody Map<String, String> body,
                                                      Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        try {
            List<String> usernames = List.of(body.get("members").split("[,;\\s]+"));
            var result = inviteService.createInvites(groupId, principal.getName(), usernames);
            var resp = new java.util.LinkedHashMap<String, Object>();
            resp.put("success", true);
            resp.put("invited", result.created().size());
            resp.put("alreadyMembers", result.alreadyMembers());
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{inviteId}/accept")
    public ResponseEntity<Map<String, Object>> accept(@PathVariable Long inviteId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        try {
            inviteService.acceptInvite(inviteId, principal.getName());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{inviteId}/decline")
    public ResponseEntity<Map<String, Object>> decline(@PathVariable Long inviteId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        try {
            inviteService.declineInvite(inviteId, principal.getName());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toDto(GroupInvite invite) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", invite.getId());
        m.put("groupId", invite.getGroup().getId());
        m.put("groupName", invite.getGroup().getName());
        m.put("invitedBy", invite.getInvitedBy().getUsername());
        m.put("invitedUser", invite.getInvitedUser().getUsername());
        m.put("status", invite.getStatus().name());
        m.put("createdAt", invite.getCreatedAt().toString());
        return m;
    }
}
