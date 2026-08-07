package com.example.testtitle.Controllers;

import com.example.testtitle.DTO.WatchRoomControlRequest;
import com.example.testtitle.DTO.WatchRoomCreateRequest;
import com.example.testtitle.DTO.WatchRoomJoinRequest;
import com.example.testtitle.DTO.WatchRoomPlaylistAddRequest;
import com.example.testtitle.DTO.WatchRoomPlaylistItemRequest;
import com.example.testtitle.Services.WatchRoomService;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
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
        watchRoomService.createRoom(principal.getName(), request.getName());
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
        watchRoomService.sendChatMessage(principal.getName(), roomId, content);
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
