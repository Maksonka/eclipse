package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.ChatGroup;
import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Repositories.ChatGroupRepository;
import com.example.shadowvibe.Repositories.MessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profile/{username}")
public class ProfileApiController {

    private final ChatGroupRepository chatGroupRepository;
    private final MessageRepository messageRepository;

    public ProfileApiController(ChatGroupRepository chatGroupRepository,
                                MessageRepository messageRepository) {
        this.chatGroupRepository = chatGroupRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping("/common-groups")
    public ResponseEntity<Map<String, Object>> commonGroups(@PathVariable String username,
                                                           Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        String viewer = principal.getName();
        if (viewer.equals(username)) {
            return ResponseEntity.ok(Map.of("count", 0, "groups", List.of()));
        }
        List<ChatGroup> groups = chatGroupRepository.findCommonGroups(viewer, username);
        List<Map<String, Object>> groupList = groups.stream().map(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("name", g.getName());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", groupList.size());
        resp.put("groups", groupList);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/shared-media")
    public ResponseEntity<Map<String, Object>> sharedMedia(@PathVariable String username,
                                                          Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        String viewer = principal.getName();
        if (viewer.equals(username)) {
            return ResponseEntity.ok(Map.of("media", List.of()));
        }
        List<Message> messages = messageRepository.findSharedMedia(
                viewer, username, PageRequest.of(0, 30));
        List<Map<String, Object>> media = messages.stream().map(m -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("type", m.getAttachmentType());
            item.put("filename", m.getAttachmentFilename());
            item.put("name", m.getAttachmentOriginalName());
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("media", media));
    }
}
