package com.example.testtitle.Controllers;

import com.example.testtitle.Models.Message;
import com.example.testtitle.Services.GroupService;
import com.example.testtitle.Services.MessageService;
import com.example.testtitle.DTO.ChatMessageDto;
import com.example.testtitle.DTO.ChatMessageRequest;
import com.example.testtitle.DTO.ChatReadRequest;
import com.example.testtitle.DTO.ChatTypingDto;
import com.example.testtitle.DTO.ChatTypingRequest;
import com.example.testtitle.DTO.GroupMessageDto;
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
                request.getContent().trim()
        );

        ChatMessageDto dto = messageService.toDto(saved);

        messagingTemplate.convertAndSendToUser(request.getReceiverUsername(), "/queue/messages", dto);
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/messages", dto);

        long unread = messageService.getUnreadCountsByPartner(request.getReceiverUsername())
                .getOrDefault(principal.getName(), 0L);
        messagingTemplate.convertAndSendToUser(request.getReceiverUsername(), "/queue/unread-update", Map.of(
                "partnerUsername", principal.getName(),
                "unreadCount", unread
        ));

        messagingTemplate.convertAndSendToUser(
                request.getReceiverUsername(),
                "/queue/typing",
                new ChatTypingDto(principal.getName(), false)
        );
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

    @MessageMapping("/group.send")
    public void sendGroupMessage(@Payload GroupMessageRequest request, Principal principal) {
        if (principal == null || request.getGroupId() == null || request.getContent() == null || request.getContent().isBlank()) {
            return;
        }

        GroupMessage saved = groupService.saveGroupMessage(
                principal.getName(),
                request.getGroupId(),
                request.getContent().trim()
        );

        GroupMessageDto dto = groupService.toDto(saved);
        String destination = "/topic/group." + request.getGroupId();
        messagingTemplate.convertAndSend(destination, dto);
    }
}
