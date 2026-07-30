package com.example.testtitle.Services;

import com.example.testtitle.DTO.ChatMessageDto;
import com.example.testtitle.DTO.ChatReadReceiptDto;
import com.example.testtitle.DTO.ConversationPreviewDto;
import com.example.testtitle.Models.Message;
import com.example.testtitle.Models.User;
import com.example.testtitle.Repositories.MessageRepository;
import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;


    public MessageService(MessageRepository messageRepository,
                          UserService userService,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }


    public Message saveMessage(String senderUsername, String receiverUsername, String content) {
        User receiver = userService.findByUsername(receiverUsername).orElseThrow(()-> new RuntimeException("пользователь не найден"));
        User sender = userService.findByUsername(senderUsername).orElseThrow(()-> new RuntimeException("пользователь не найден"));


        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        return messageRepository.save(message);
    }

    public List<Message> getChatHistory(String senderUsername,String receiverUsername) {
        return messageRepository.findChatHistory(senderUsername,receiverUsername);
    }

    public ChatMessageDto toDto(Message message) {
        String time = message.getTimestamp() != null
                ? message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "";
        return new ChatMessageDto(
                message.getId(),
                message.getContent(),
                message.getSender().getUsername(),
                message.getReceiver().getUsername(),
                time,
                message.isRead()
        );
    }

    public List<ConversationPreviewDto> getConversations(String username) {
        Map<String, Long> unreadByPartner = getUnreadCountsByPartner(username);
        List<Message> messages = messageRepository.findAllByUserInvolvedOrderByTimestampDesc(username);
        Map<String, Message> latestByPartner = new LinkedHashMap<>();

        for (Message message : messages) {
            String partner = message.getSender().getUsername().equals(username)
                    ? message.getReceiver().getUsername()
                    : message.getSender().getUsername();
            latestByPartner.putIfAbsent(partner, message);
        }

        List<ConversationPreviewDto> conversations = new ArrayList<>();
        for (Message message : latestByPartner.values()) {
            conversations.add(toConversationPreview(message, username, unreadByPartner));
        }
        return conversations;
    }

    public Map<String, Long> getUnreadCountsByPartner(String username) {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : messageRepository.countUnreadByPartner(username)) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }

    public long getTotalUnreadCount(String username) {
        return getUnreadCountsByPartner(username).values().stream().mapToLong(Long::longValue).sum();
    }

    public List<Long> markConversationAsRead(String readerUsername, String partnerUsername) {
        List<Message> unread = messageRepository.findUnreadFromPartner(readerUsername, partnerUsername);
        if (unread.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        messageRepository.markConversationAsRead(readerUsername, partnerUsername, now);

        List<Long> messageIds = unread.stream().map(Message::getId).toList();
        notifyReadReceipt(readerUsername, partnerUsername, messageIds);
        return messageIds;
    }

    public void notifyReadReceipt(String readerUsername, String partnerUsername, List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        ChatReadReceiptDto receipt = new ChatReadReceiptDto(readerUsername, partnerUsername, messageIds);
        messagingTemplate.convertAndSendToUser(partnerUsername, "/queue/read-receipts", receipt);
        messagingTemplate.convertAndSendToUser(readerUsername, "/queue/unread-update", Map.of(
                "partnerUsername", partnerUsername,
                "unreadCount", 0L
        ));
    }

    private ConversationPreviewDto toConversationPreview(Message message, String currentUsername, Map<String, Long> unreadByPartner) {
        User partnerUser = message.getSender().getUsername().equals(currentUsername)
                ? message.getReceiver()
                : message.getSender();
        String partner = partnerUser.getUsername();
        boolean outgoing = message.getSender().getUsername().equals(currentUsername);

        String preview = message.getContent();
        if (preview.length() > 48) {
            preview = preview.substring(0, 48) + "…";
        }

        String time = message.getTimestamp() != null
                ? message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "";

        long unread = unreadByPartner.getOrDefault(partner, 0L);

        return new ConversationPreviewDto(
                partner,
                preview,
                time,
                outgoing,
                partnerUser.getAvatarFilename(),
                unread
        );
    }
}
