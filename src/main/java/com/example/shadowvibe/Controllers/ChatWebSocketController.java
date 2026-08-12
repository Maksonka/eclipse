package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.Message;
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
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService,
                                   GroupService groupService,
                                   ReactionService reactionService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.groupService = groupService;
        this.reactionService = reactionService;
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
                    request.getReplyToMessageId()
            );
        } else {
            saved = messageService.saveMessage(
                    principal.getName(),
                    request.getReceiverUsername(),
                    request.getContent().trim(),
                    request.getReplyToMessageId()
            );
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
        if (request.getContent() == null || request.getContent().isBlank()) {
            if (!hasSticker) {
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
}