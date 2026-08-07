package com.example.testtitle.Controllers;

import com.example.testtitle.DTO.GroupMessageDto;
import com.example.testtitle.DTO.GroupPreviewDto;
import com.example.testtitle.Models.ChatGroup;
import com.example.testtitle.Models.GroupMessage;
import com.example.testtitle.Models.User;
import com.example.testtitle.Services.GroupService;
import com.example.testtitle.Services.PresenceService;
import com.example.testtitle.Services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupApiController {

    private final GroupService groupService;
    private final UserService userService;
    private final PresenceService presenceService;

    public GroupApiController(GroupService groupService,
                              UserService userService,
                              PresenceService presenceService) {
        this.groupService = groupService;
        this.userService = userService;
        this.presenceService = presenceService;
    }

    @GetMapping
    public ResponseEntity<?> list(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        List<GroupPreviewDto> groups = groupService.getGroupPreviews(principal.getName());
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        String name = body.get("name");
        String members = body.get("members");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Укажите название группы"));
        }
        try {
            ChatGroup group = groupService.createGroup(principal.getName(), name, members);
            return ResponseEntity.ok(Map.of("groupId", group.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> get(@PathVariable Long groupId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        ChatGroup group = groupService.getGroupForMember(groupId, principal.getName());
        List<User> members = groupService.getGroupMembers(groupId, principal.getName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", group.getId());
        result.put("name", group.getName());
        result.put("avatarFilename", group.getAvatarFilename());
        result.put("createdBy", group.getCreatedBy().getUsername());
        result.put("isCreator", group.getCreatedBy().getUsername().equals(principal.getName()));
        result.put("members", members.stream().map(this::toUserMap).toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<?> history(@PathVariable Long groupId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        List<GroupMessageDto> messages = groupService.getGroupHistory(groupId, principal.getName())
                .stream().map(groupService::toDto).toList();
        groupService.markGroupAsRead(principal.getName(), groupId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{groupId}/read")
    public ResponseEntity<?> markRead(@PathVariable Long groupId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        groupService.markGroupAsRead(principal.getName(), groupId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{groupId}/members/add")
    public ResponseEntity<?> addMembers(@PathVariable Long groupId,
                                        @RequestBody Map<String, String> body,
                                        Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        try {
            groupService.addMembers(groupId, principal.getName(), body.get("members"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/members/{username}/remove")
    public ResponseEntity<?> removeMember(@PathVariable Long groupId,
                                          @PathVariable String username,
                                          Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        try {
            groupService.removeMember(groupId, principal.getName(), username);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/delete")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        try {
            groupService.deleteGroup(groupId, principal.getName());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/avatar")
    public ResponseEntity<?> updateAvatar(@PathVariable Long groupId,
                                          @RequestParam("avatar") MultipartFile avatar,
                                          Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        try {
            ChatGroup group = groupService.updateGroupAvatar(groupId, principal.getName(), avatar);
            return ResponseEntity.ok(Map.of("success", true, "avatarFilename", group.getAvatarFilename()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Не удалось сохранить аватар"));
        }
    }

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("avatarFilename", user.getAvatarFilename());
        map.put("about", user.getAbout());
        map.put("online", presenceService.isOnline(user.getUsername()));
        return map;
    }
}
