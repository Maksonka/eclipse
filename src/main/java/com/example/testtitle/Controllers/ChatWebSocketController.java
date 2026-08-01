package com.example.testtitle.Controllers;

import com.example.testtitle.Models.Message;
import com.example.testtitle.Services.GroupService;
import com.example.testtitle.Services.MessageService;
import com.example.testtitle.DTO.ChatMessageRequest;
import com.example.testtitle.DTO.ChatReadRequest;
import com.example.testtitle.DTO.ChatTypingDto;
import com.example.testtitle.DTO.ChatTypingRequest;
import com.example.testtitle.DTO.GroupMessageRequest;
import com.example.testtitle.Models.GroupMessage;
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
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService,
                                   GroupService groupService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.groupService = groupService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        if (principal == null || request.getContent() == null || request.getContent().isBlank()) {
            return;
        }

        Message saved = messageService.saveMessage(
                principal.getName(),
                request.getReceiverUsername(),
                request.getContent().trim(),
                request.getReplyToMessageId()
        );

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
        if (principal == null || request.getGroupId() == null || request.getContent() == null || request.getContent().isBlank()) {
            return;
        }

        GroupMessage saved = groupService.saveGroupMessage(
                principal.getName(),
                request.getGroupId(),
                request.getContent().trim(),
                request.getReplyToMessageId()
        );

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
}