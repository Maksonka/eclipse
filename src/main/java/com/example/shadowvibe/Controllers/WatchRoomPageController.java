package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Services.WatchRoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/watch")
public class WatchRoomPageController {

    private final WatchRoomService watchRoomService;

    public WatchRoomPageController(WatchRoomService watchRoomService) {
        this.watchRoomService = watchRoomService;
    }

    @GetMapping
    public String watchPage(@RequestParam(value = "room", required = false) String room,
                            Principal principal, Model model) {
        model.addAttribute("rooms", watchRoomService.getRoomPreviews());
        model.addAttribute("room", room);
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
}
