package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Services.WatchRoomService;
import com.example.shadowvibe.Services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/watch")
public class WatchRoomPageController {

    private final WatchRoomService watchRoomService;
    private final UserService userService;

    public WatchRoomPageController(WatchRoomService watchRoomService, UserService userService) {
        this.watchRoomService = watchRoomService;
        this.userService = userService;
    }

    @GetMapping
    public String watchPage(@RequestParam(value = "room", required = false) String room,
                            Principal principal, Model model) {
        model.addAttribute("rooms", watchRoomService.getRoomPreviews());
        model.addAttribute("room", room);
        if (principal != null) {
            model.addAttribute("currentUser", userService.findByUsername(principal.getName()).orElse(null));
        }
        return "watch";
    }

    @GetMapping("/{roomId}/messages")
    @ResponseBody
    public ResponseEntity<?> roomMessages(@PathVariable Long roomId, Principal principal) {
        try {
            return ResponseEntity.ok(watchRoomService.getChatHistory(principal.getName(), roomId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/playlist")
    @ResponseBody
    public ResponseEntity<?> roomPlaylist(@PathVariable Long roomId, Principal principal) {
        try {
            return ResponseEntity.ok(watchRoomService.getPlaylist(principal.getName(), roomId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/rooms")
    @ResponseBody
    public ResponseEntity<?> apiRooms(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизовано"));
        }
        return ResponseEntity.ok(watchRoomService.getRoomPreviews());
    }

    @PostMapping("/api/rooms/create")
    @ResponseBody
    public ResponseEntity<?> apiCreateRoom(@RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизовано"));
        }
        String name = body.get("name") != null ? body.get("name").toString() : "Комната";
        boolean publicRoom = !"private".equalsIgnoreCase(
                body.get("visibility") != null ? body.get("visibility").toString() : "public");
        try {
            var dto = watchRoomService.createRoom(principal.getName(), name, publicRoom ? "public" : "private");
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("roomId", dto.getRoomId());
            resp.put("roomCode", dto.getRoomCode());
            resp.put("name", dto.getName());
            resp.put("hostUsername", dto.getHostUsername());
            resp.put("status", dto.getStatus() != null ? dto.getStatus().name() : "PAUSED");
            resp.put("members", dto.getMembers() != null ? dto.getMembers() : java.util.List.of(principal.getName()));
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/rooms/join")
    @ResponseBody
    public ResponseEntity<?> apiJoinRoom(@RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизовано"));
        }
        String code = body.get("roomCode") != null ? body.get("roomCode").toString() : "";
        try {
            var dto = watchRoomService.joinRoom(principal.getName(), code);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("roomId", dto.getRoomId());
            resp.put("roomCode", dto.getRoomCode());
            resp.put("name", dto.getName());
            resp.put("hostUsername", dto.getHostUsername());
            resp.put("videoUrl", dto.getVideoUrl());
            resp.put("status", dto.getStatus() != null ? dto.getStatus().name() : "PAUSED");
            resp.put("positionMs", dto.getPositionMs());
            resp.put("restart", dto.isRestart());
            resp.put("members", dto.getMembers() != null ? dto.getMembers() : java.util.List.of());
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/rooms/{roomId}/leave")
    @ResponseBody
    public ResponseEntity<?> apiLeaveRoom(@PathVariable Long roomId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизовано"));
        }
        try {
            watchRoomService.leaveRoom(principal.getName(), roomId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
