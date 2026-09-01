package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Services.FavoriteService;
import com.example.shadowvibe.Services.BlockService;
import com.example.shadowvibe.Services.GroupService;
import com.example.shadowvibe.Services.MessageService;
import com.example.shadowvibe.Services.ReactionService;
import com.example.shadowvibe.DTO.ChatMessageRequest;
import com.example.shadowvibe.DTO.ChatReadRequest;
import com.example.shadowvibe.DTO.ChatTypingDto;
import com.example.shadowvibe.DTO.ChatTypingRequest;
import com.example.shadowvibe.DTO.GroupMessageRequest;
import com.example.shadowvibe.DTO.ReactionEventDto;
import com.example.shadowvibe.Models.GroupMessage;
import com.example.shadowvibe.enums.ReactionTargetType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class ChatWebSocketController {

    private final MessageService messageService;
    private final GroupService groupService;
    private final ReactionService reactionService;
    private final FavoriteService favoriteService;
    private final BlockService blockService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService,
                                   GroupService groupService,
                                   ReactionService reactionService,
                                   FavoriteService favoriteService,
                                   BlockService blockService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.groupService = groupService;
        this.reactionService = reactionService;
        this.favoriteService = favoriteService;
        this.blockService = blockService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        if (principal == null || request.getReceiverUsername() == null || request.getReceiverUsername().isBlank()) {
            return;
        }
        if (request.getReceiverUsername().equals(principal.getName())) {
            return;
        }

        boolean hasSticker = request.getStickerCode() != null && !request.getStickerCode().isBlank();
        boolean hasAudio = request.getAudioUrl() != null && !request.getAudioUrl().isBlank();
        if (request.getContent() == null || request.getContent().isBlank()) {
            if (!hasSticker && !hasAudio) {
                return;
            }
        }

        Message saved;
        try {
            blockService.assertCanMessage(principal.getName(), request.getReceiverUsername());
            if (hasSticker) {
                saved = messageService.saveStickerMessage(
                        principal.getName(),
                        request.getReceiverUsername(),
                        request.getStickerCode().trim()
                );
            } else if (hasAudio) {
                saved = messageService.saveAudioMessage(
                        principal.getName(),
                        request.getReceiverUsername(),
                        request.getAudioUrl().trim(),
                        request.getReplyToMessageId(),
                        request.getAudioDurationMs()
                );
            } else {
                saved = messageService.saveMessage(
                        principal.getName(),
                        request.getReceiverUsername(),
                        request.getContent().trim(),
                        request.getReplyToMessageId()
                );
            }
        } catch (IllegalArgumentException e) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/chat-error",
                    Map.of("error", e.getMessage()));
            return;
        }

        messageService.broadcastDirectMessage(saved);
    }

    @MessageMapping("/chat.read")
    public void markRead(@Payload ChatReadRequest request, Principal principal) {
        if (principal == null || request.getPartnerUsername() == null || request.getPartnerUsername().isBlank()) {
            return;
        }

        messageService.markConversationAsRead(principal.getName(), request.getPartnerUsername());
    }

    @MessageMapping("/chat.typing")
    public void sendTyping(@Payload ChatTypingRequest request, Principal principal) {
        if (principal == null || request.getReceiverUsername() == null || request.getReceiverUsername().isBlank()) {
            return;
        }
        if (!blockService.canMessage(principal.getName(), request.getReceiverUsername())) {
            return;
        }

        ChatTypingDto dto = new ChatTypingDto(principal.getName(), request.isTyping());
        messagingTemplate.convertAndSendToUser(request.getReceiverUsername(), "/queue/typing", dto);
    }

    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null || request.get("mode") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        String mode = request.get("mode").toString();
        String username = principal.getName();

        if ("everyone".equals(mode)) {
            messageService.deleteMessageForEveryone(messageId, username);
        } else {
            messageService.deleteMessageForMe(messageId, username);
        }
    }

    @MessageMapping("/group.send")
    public void sendGroupMessage(@Payload GroupMessageRequest request, Principal principal) {
        if (principal == null || request.getGroupId() == null) {
            return;
        }

        boolean hasSticker = request.getStickerCode() != null && !request.getStickerCode().isBlank();
        boolean hasAudio = request.getAudioUrl() != null && !request.getAudioUrl().isBlank();
        if (request.getContent() == null || request.getContent().isBlank()) {
            if (!hasSticker && !hasAudio) {
                return;
            }
        }

        GroupMessage saved;
        if (hasSticker) {
            saved = groupService.saveGroupStickerMessage(
                    principal.getName(),
                    request.getGroupId(),
                    request.getStickerCode().trim()
            );
        } else if (hasAudio) {
            saved = groupService.saveGroupAudioMessage(
                    principal.getName(),
                    request.getGroupId(),
                    request.getAudioUrl().trim(),
                    request.getAudioDurationMs(),
                    request.getReplyToMessageId()
            );
        } else {
            saved = groupService.saveGroupMessage(
                    principal.getName(),
                    request.getGroupId(),
                    request.getContent().trim(),
                    request.getReplyToMessageId()
            );
        }

        groupService.broadcastGroupMessage(saved);
    }

    @MessageMapping("/group.typing")
    public void sendGroupTyping(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("groupId") == null || request.get("typing") == null) {
            return;
        }
        Long groupId;
        try {
            groupId = Long.valueOf(request.get("groupId").toString());
        } catch (NumberFormatException e) {
            return;
        }
        boolean typing = Boolean.parseBoolean(request.get("typing").toString());
        ChatTypingDto dto = new ChatTypingDto(principal.getName(), typing);
        messagingTemplate.convertAndSend("/topic/group." + groupId + ".typing", dto);
    }

    @MessageMapping("/group.delete")
    public void deleteGroupMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null || request.get("mode") == null || request.get("groupId") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        Long groupId = Long.valueOf(request.get("groupId").toString());
        String mode = request.get("mode").toString();
        String username = principal.getName();

        var msg = "everyone".equals(mode)
                ? groupService.deleteGroupMessageForEveryone(messageId, username)
                : groupService.deleteGroupMessageForMe(messageId, username);
        messagingTemplate.convertAndSend("/topic/group." + groupId, msg);
    }

    @MessageMapping("/chat.react")
    public void reactToDirectMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null || request.get("emoji") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        String emoji = request.get("emoji").toString();
        var reactions = reactionService.toggle(ReactionTargetType.DIRECT, messageId, principal.getName(), emoji);

        var usernames = messageService.getDirectMessageUsernames(messageId);
        if (usernames.isEmpty()) {
            return;
        }
        ReactionEventDto event = new ReactionEventDto(messageId, "DIRECT", reactions);
        messagingTemplate.convertAndSendToUser(usernames.get().getKey(), "/queue/reactions", event);
        messagingTemplate.convertAndSendToUser(usernames.get().getValue(), "/queue/reactions", event);
    }

    @MessageMapping("/group.react")
    public void reactToGroupMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null || request.get("groupId") == null
                || request.get("emoji") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        Long groupId = Long.valueOf(request.get("groupId").toString());
        String emoji = request.get("emoji").toString();

        var reactions = reactionService.toggle(ReactionTargetType.GROUP, messageId, principal.getName(), emoji);
        ReactionEventDto event = new ReactionEventDto(messageId, "GROUP", reactions);
        messagingTemplate.convertAndSend("/topic/group." + groupId + ".reactions", event);
    }

    @MessageMapping("/chat.edit")
    public void editDirectMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null || request.get("content") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        messageService.editMessage(messageId, principal.getName(), request.get("content").toString());
    }

    @MessageMapping("/chat.transcribe")
    public void transcribeDirectMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        String transcript = request.get("transcript") != null ? request.get("transcript").toString() : null;
        try {
            messageService.transcribeMessage(messageId, principal.getName(), transcript);
        } catch (RuntimeException e) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/chat-error",
                    Map.of("error", e.getMessage()));
        }
    }

    @MessageMapping("/group.transcribe")
    public void transcribeGroupMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null || request.get("groupId") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        Long groupId = Long.valueOf(request.get("groupId").toString());
        String transcript = request.get("transcript") != null ? request.get("transcript").toString() : null;
        try {
            groupService.transcribeGroupMessage(messageId, groupId, principal.getName(), transcript);
        } catch (RuntimeException e) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/chat-error",
                    Map.of("error", e.getMessage()));
        }
    }

    @MessageMapping("/group.edit")
    public void editGroupMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null || request.get("groupId") == null
                || request.get("content") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        Long groupId = Long.valueOf(request.get("groupId").toString());
        groupService.editGroupMessage(messageId, groupId, principal.getName(), request.get("content").toString());
    }

    @MessageMapping("/chat.pin")
    public void pinDirectMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null || request.get("pinned") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        boolean pinned = Boolean.parseBoolean(request.get("pinned").toString());
        if (pinned) {
            messageService.pinDirectMessage(messageId, principal.getName());
        } else {
            messageService.unpinDirectMessage(messageId, principal.getName());
        }
    }

    @MessageMapping("/group.pin")
    public void pinGroupMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null || request.get("groupId") == null
                || request.get("pinned") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        Long groupId = Long.valueOf(request.get("groupId").toString());
        boolean pinned = Boolean.parseBoolean(request.get("pinned").toString());
        if (pinned) {
            groupService.pinGroupMessage(messageId, groupId, principal.getName());
        } else {
            groupService.unpinGroupMessage(messageId, groupId, principal.getName());
        }
    }

    @MessageMapping("/chat.favorite")
    public void favoriteDirectMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        try {
            var dto = favoriteService.toggleDirect(messageId, principal.getName());
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/favorites", dto);
        } catch (RuntimeException e) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/favorites",
                    Map.of("error", e.getMessage()));
        }
    }

    @MessageMapping("/group.favorite")
    public void favoriteGroupMessage(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("messageId") == null) {
            return;
        }
        Long messageId = Long.valueOf(request.get("messageId").toString());
        try {
            var dto = favoriteService.toggleGroup(messageId, principal.getName());
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/favorites", dto);
        } catch (RuntimeException e) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/favorites",
                    Map.of("error", e.getMessage()));
        }
    }

    @MessageMapping("/chat.forward")
    public void forwardToDirect(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("sourceMessageId") == null || request.get("targetUsername") == null) {
            return;
        }
        String sourceType = request.get("sourceType") != null ? request.get("sourceType").toString() : "DIRECT";
        Long sourceMessageId = Long.valueOf(request.get("sourceMessageId").toString());
        String targetUsername = request.get("targetUsername").toString();

        try {
            blockService.assertCanMessage(principal.getName(), targetUsername);
            var forward = messageService.forwardToUser(sourceType, sourceMessageId, targetUsername, principal.getName());
            messageService.broadcastDirectMessage(forward);
        } catch (IllegalArgumentException e) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/chat-error",
                    Map.of("error", e.getMessage()));
        }
    }

    @MessageMapping("/group.forward")
    public void forwardToGroup(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null || request.get("sourceMessageId") == null || request.get("groupId") == null) {
            return;
        }
        String sourceType = request.get("sourceType") != null ? request.get("sourceType").toString() : "GROUP";
        Long sourceMessageId = Long.valueOf(request.get("sourceMessageId").toString());
        Long groupId = Long.valueOf(request.get("groupId").toString());

        var forward = groupService.forwardToGroup(sourceType, sourceMessageId, groupId, principal.getName());
        groupService.broadcastGroupMessage(forward);
    }
}