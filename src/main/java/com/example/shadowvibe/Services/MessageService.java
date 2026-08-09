package com.example.shadowvibe.Services;

import com.example.shadowvibe.DTO.ChatMessageDto;
import com.example.shadowvibe.DTO.ChatReadReceiptDto;
import com.example.shadowvibe.DTO.ChatTypingDto;
import com.example.shadowvibe.DTO.ConversationPreviewDto;
import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.Sticker;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.MessageRepository;
import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AttachmentService attachmentService;
    private final StickerService stickerService;


    public MessageService(MessageRepository messageRepository,
                          UserService userService,
                          SimpMessagingTemplate messagingTemplate,
                          AttachmentService attachmentService,
                          StickerService stickerService) {
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
        this.attachmentService = attachmentService;
        this.stickerService = stickerService;
    }


    public Message saveMessage(String senderUsername, String receiverUsername, String content, Long replyToMessageId) {
        User receiver = userService.findByUsername(receiverUsername).orElseThrow(()-> new RuntimeException("пользователь не найден"));
        User sender = userService.findByUsername(senderUsername).orElseThrow(()-> new RuntimeException("пользователь не найден"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());

        if (replyToMessageId != null) {
            Message original = messageRepository.findById(replyToMessageId).orElse(null);
            if (original != null) {
                message.setReplyToMessageId(original.getId());
                message.setReplyToContent(original.getContent() != null ? original.getContent() : "");
                message.setReplyToSenderUsername(original.getSender().getUsername());
            }
        }

        return messageRepository.save(message);
    }

    public Message saveAttachmentMessage(String senderUsername, String receiverUsername, MultipartFile file) throws IOException {
        AttachmentService.AttachmentInfo info = attachmentService.save(file);

        User receiver = userService.findByUsername(receiverUsername)
                .orElseThrow(() -> new RuntimeException("пользователь не найден"));
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("пользователь не найден"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("");
        message.setTimestamp(LocalDateTime.now());
        message.setAttachmentFilename(info.filename());
        message.setAttachmentType(info.type());
        message.setAttachmentOriginalName(info.originalName());
        message.setAttachmentSize(info.size());
        return messageRepository.save(message);
    }

    public Message saveStickerMessage(String senderUsername, String receiverUsername, String stickerCode) {
        Sticker sticker = stickerService.findByCode(stickerCode);
        if (sticker == null) {
            throw new RuntimeException("Стикер не найден");
        }

        User receiver = userService.findByUsername(receiverUsername)
                .orElseThrow(() -> new RuntimeException("пользователь не найден"));
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("пользователь не найден"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("");
        message.setTimestamp(LocalDateTime.now());
        message.setStickerCode(sticker.getCode());
        message.setStickerUrl(sticker.getFilename());
        return messageRepository.save(message);
    }

    public ChatMessageDto broadcastDirectMessage(Message message) {
        ChatMessageDto dto = toDto(message);

        messagingTemplate.convertAndSendToUser(message.getReceiver().getUsername(), "/queue/messages", dto);
        messagingTemplate.convertAndSendToUser(message.getSender().getUsername(), "/queue/messages", dto);

        long unread = getUnreadCountsByPartner(message.getReceiver().getUsername())
                .getOrDefault(message.getSender().getUsername(), 0L);
        messagingTemplate.convertAndSendToUser(message.getReceiver().getUsername(), "/queue/unread-update", Map.of(
                "partnerUsername", message.getSender().getUsername(),
                "unreadCount", unread
        ));

        messagingTemplate.convertAndSendToUser(
                message.getReceiver().getUsername(),
                "/queue/typing",
                new ChatTypingDto(message.getSender().getUsername(), false)
        );
        return dto;
    }

    public List<Message> getChatHistory(String senderUsername,String receiverUsername) {
        return messageRepository.findChatHistory(senderUsername,receiverUsername);
    }

    public void deleteConversation(String username, String partnerUsername) {
        messageRepository.deleteConversation(username, partnerUsername);
        messagingTemplate.convertAndSendToUser(partnerUsername, "/queue/conversation-deleted", Map.of(
                "deletedByUsername", username
        ));
    }

    public ChatMessageDto toDto(Message message) {
        String time = message.getTimestamp() != null
                ? message.getTimestamp().format(TIME_FORMATTER)
                : "";
        boolean deleted = message.isDeletedBySender() && message.isDeletedByReceiver();
        ChatMessageDto dto = new ChatMessageDto(
                message.getId(),
                message.getContent(),
                message.getSender().getUsername(),
                message.getReceiver().getUsername(),
                time,
                message.isRead(),
                message.getAttachmentFilename(),
                message.getAttachmentType(),
                message.getAttachmentOriginalName(),
                message.getAttachmentSize(),
                message.getReplyToMessageId(),
                message.getReplyToContent(),
                message.getReplyToSenderUsername(),
                deleted
        );
        dto.setStickerCode(message.getStickerCode());
        dto.setStickerUrl(message.getStickerUrl());
        return dto;
    }

    public List<ConversationPreviewDto> getConversations(String username) {
        return getConversations(username, getUnreadCountsByPartner(username));
    }

    public List<ConversationPreviewDto> getConversations(String username, Map<String, Long> unreadByPartner) {
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

    public long getTotalUnreadCount(Map<String, Long> unreadByPartner) {
        return unreadByPartner.values().stream().mapToLong(Long::longValue).sum();
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

    public ChatMessageDto deleteMessageForMe(Long messageId, String username) {
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        if (message.getSender().getUsername().equals(username)) {
            message.setDeletedBySender(true);
        } else {
            message.setDeletedByReceiver(true);
        }
        messageRepository.save(message);
        return toDto(message);
    }

    public ChatMessageDto deleteMessageForEveryone(Long messageId, String username) {
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        if (!message.getSender().getUsername().equals(username)) {
            throw new RuntimeException("Только автор может удалить сообщение для всех");
        }
        message.setContent("Сообщение удалено");
        message.setAttachmentFilename(null);
        message.setAttachmentType(null);
        message.setAttachmentOriginalName(null);
        message.setAttachmentSize(null);
        message.setStickerCode(null);
        message.setStickerUrl(null);
        message.setDeletedBySender(true);
        message.setDeletedByReceiver(true);
        messageRepository.save(message);
        ChatMessageDto dto = toDto(message);
        messagingTemplate.convertAndSendToUser(message.getReceiver().getUsername(), "/queue/messages", dto);
        messagingTemplate.convertAndSendToUser(message.getSender().getUsername(), "/queue/messages", dto);
        return dto;
    }

    private ConversationPreviewDto toConversationPreview(Message message, String currentUsername, Map<String, Long> unreadByPartner) {
        User partnerUser = message.getSender().getUsername().equals(currentUsername)
                ? message.getReceiver()
                : message.getSender();
        String partner = partnerUser.getUsername();
        boolean outgoing = message.getSender().getUsername().equals(currentUsername);

        String preview = message.hasSticker() ? "Стикер"
                : (message.getContent() != null && !message.getContent().isBlank()
                        ? message.getContent()
                        : AttachmentService.labelForType(message.getAttachmentType()));
        if (preview.length() > 48) {
            preview = preview.substring(0, 48) + "…";
        }

        String time = message.getTimestamp() != null
                ? message.getTimestamp().format(TIME_FORMATTER)
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
