package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.DTO.WatchRoomControlRequest;
import com.example.shadowvibe.DTO.WatchRoomCreateRequest;
import com.example.shadowvibe.DTO.WatchRoomJoinRequest;
import com.example.shadowvibe.DTO.WatchRoomPlaylistAddRequest;
import com.example.shadowvibe.DTO.WatchRoomPlaylistItemRequest;
import com.example.shadowvibe.Services.WatchRoomService;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
public class WatchRoomWebSocketController {

    private final WatchRoomService watchRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    public WatchRoomWebSocketController(WatchRoomService watchRoomService,
                                        SimpMessagingTemplate messagingTemplate) {
        this.watchRoomService = watchRoomService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/room.create")
    public void createRoom(@Payload WatchRoomCreateRequest request, Principal principal) {
        if (principal == null) {
            return;
        }
        watchRoomService.createRoom(principal.getName(), request.getName(), request.getVisibility());
    }

    @MessageMapping("/room.join")
    public void joinRoom(@Payload WatchRoomJoinRequest request, Principal principal) {
        if (principal == null) {
            return;
        }
        watchRoomService.joinRoom(principal.getName(), request.getRoomCode());
    }

    @MessageMapping("/room.leave")
    public void leaveRoom(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("roomId") == null) {
            return;
        }
        watchRoomService.leaveRoom(principal.getName(), Long.valueOf(request.get("roomId").toString()));
    }

    @MessageMapping("/room.control")
    public void control(@Payload WatchRoomControlRequest request, Principal principal) {
        if (principal == null || request.getRoomId() == null) {
            return;
        }
        watchRoomService.updateControl(principal.getName(), request.getRoomId(), request);
    }

    @MessageMapping("/room.request-state")
    public void requestState(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("roomId") == null) {
            return;
        }
        watchRoomService.requestState(principal.getName(), Long.valueOf(request.get("roomId").toString()));
    }

    @MessageMapping("/room.message")
    public void sendMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("roomId") == null) {
            return;
        }
        Long roomId = Long.valueOf(request.get("roomId").toString());
        String content = request.get("content") != null ? request.get("content").toString() : null;
        String stickerCode = request.get("stickerCode") != null ? request.get("stickerCode").toString() : null;
        String audioUrl = request.get("audioUrl") != null ? request.get("audioUrl").toString() : null;
        Long audioDurationMs = request.get("audioDurationMs") != null ? Long.valueOf(request.get("audioDurationMs").toString()) : null;
        watchRoomService.sendChatMessage(principal.getName(), roomId, content, stickerCode, audioUrl, audioDurationMs);
    }

    @MessageMapping("/room.voice.join")
    public void voiceJoin(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("roomId") == null) {
            return;
        }
        watchRoomService.voiceJoin(principal.getName(), Long.valueOf(request.get("roomId").toString()));
    }

    @MessageMapping("/room.voice.leave")
    public void voiceLeave(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("roomId") == null) {
            return;
        }
        watchRoomService.voiceLeave(principal.getName(), Long.valueOf(request.get("roomId").toString()));
    }

    @MessageMapping("/room.voice.signal")
    public void voiceSignal(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("roomId") == null || request.get("to") == null) {
            return;
        }
        Long roomId = Long.valueOf(request.get("roomId").toString());
        String target = request.get("to").toString();
        String type = request.get("type") != null ? request.get("type").toString() : null;
        Map<String, Object> payload = new HashMap<>();
        if (request.get("sdp") != null) {
            payload.put("sdp", request.get("sdp"));
        }
        if (request.get("candidate") != null) {
            payload.put("candidate", request.get("candidate"));
        }
        watchRoomService.voiceSignal(principal.getName(), roomId, target, type, payload);
    }

    @MessageMapping("/room.react")
    public void react(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("roomId") == null) {
            return;
        }
        Long roomId = Long.valueOf(request.get("roomId").toString());
        String emoji = request.get("emoji") != null ? request.get("emoji").toString() : null;
        watchRoomService.react(principal.getName(), roomId, emoji);
    }

    @MessageMapping("/room.playlist.add")
    public void playlistAdd(@Payload WatchRoomPlaylistAddRequest request, Principal principal) {
        if (principal == null || request.getRoomId() == null) {
            return;
        }
        watchRoomService.addPlaylistItem(principal.getName(), request.getRoomId(), request.getUrl(), request.getTitle());
    }

    @MessageMapping("/room.playlist.remove")
    public void playlistRemove(@Payload WatchRoomPlaylistItemRequest request, Principal principal) {
        if (principal == null || request.getRoomId() == null || request.getItemId() == null) {
            return;
        }
        watchRoomService.removePlaylistItem(principal.getName(), request.getRoomId(), request.getItemId());
    }

    @MessageMapping("/room.playlist.play")
    public void playlistPlay(@Payload WatchRoomPlaylistItemRequest request, Principal principal) {
        if (principal == null || request.getRoomId() == null || request.getItemId() == null) {
            return;
        }
        watchRoomService.playPlaylistItem(principal.getName(), request.getRoomId(), request.getItemId());
    }

    @MessageMapping("/room.playlist.next")
    public void playlistNext(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("roomId") == null) {
            return;
        }
        watchRoomService.nextPlaylistItem(principal.getName(), Long.valueOf(request.get("roomId").toString()));
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    public void handleIllegalArgument(IllegalArgumentException e, Principal principal) {
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/room-error",
                    Map.of("error", e.getMessage() != null ? e.getMessage() : "Произошла ошибка")
            );
        }
    }
}
