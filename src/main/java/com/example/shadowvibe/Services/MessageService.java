package com.example.shadowvibe.Services;

import com.example.shadowvibe.DTO.ChatMessageDto;
import com.example.shadowvibe.DTO.ChatReadReceiptDto;
import com.example.shadowvibe.DTO.ChatTypingDto;
import com.example.shadowvibe.DTO.ConversationPreviewDto;
import com.example.shadowvibe.DTO.MessageSearchResultDto;
import com.example.shadowvibe.Models.GroupMessage;
import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.Sticker;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.GroupMessageRepository;
import com.example.shadowvibe.Repositories.MessageRepository;
import com.example.shadowvibe.enums.ReactionTargetType;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class MessageService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final MessageRepository messageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AttachmentService attachmentService;
    private final StickerService stickerService;
    private final ReactionService reactionService;
    private final PushService pushService;
    private final PresenceService presenceService;
    private final MuteService muteService;
    private final FavoriteService favoriteService;
    private final PremiumService premiumService;


    public MessageService(MessageRepository messageRepository,
                          GroupMessageRepository groupMessageRepository,
                          UserService userService,
                          SimpMessagingTemplate messagingTemplate,
                          AttachmentService attachmentService,
                          StickerService stickerService,
                          ReactionService reactionService,
                          PushService pushService,
                          PresenceService presenceService,
                          MuteService muteService,
                          FavoriteService favoriteService,
                          PremiumService premiumService) {
        this.messageRepository = messageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
        this.attachmentService = attachmentService;
        this.stickerService = stickerService;
        this.reactionService = reactionService;
        this.pushService = pushService;
        this.presenceService = presenceService;
        this.muteService = muteService;
        this.favoriteService = favoriteService;
        this.premiumService = premiumService;
    }


    private String replyPreviewText(Message original) {
        if (original == null) {
            return "";
        }
        if (original.hasAudio()) {
            return "Голосовое сообщение";
        }
        if (original.hasSticker()) {
            return "Стикер";
        }
        return original.getContent() != null ? original.getContent() : "";
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
                message.setReplyToContent(replyPreviewText(original));
                message.setReplyToSenderUsername(original.getSender().getUsername());
            }
        }

        return messageRepository.save(message);
    }

    public Message saveAttachmentMessage(String senderUsername, String receiverUsername, MultipartFile file) throws IOException {
        return saveAttachmentMessage(senderUsername, receiverUsername, file, null, null);
    }

    public Message saveAttachmentMessage(String senderUsername, String receiverUsername, MultipartFile file,
                                         String content, Long replyToMessageId) throws IOException {
        premiumService.enforceFileSize(senderUsername, file.getSize());
        AttachmentService.AttachmentInfo info = attachmentService.save(file);

        User receiver = userService.findByUsername(receiverUsername)
                .orElseThrow(() -> new RuntimeException("пользователь не найден"));
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("пользователь не найден"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content == null ? "" : content.trim());
        message.setTimestamp(LocalDateTime.now());
        message.setAttachmentFilename(info.filename());
        message.setAttachmentType(info.type());
        message.setAttachmentOriginalName(info.originalName());
        message.setAttachmentSize(info.size());

        if (replyToMessageId != null) {
            Message original = messageRepository.findById(replyToMessageId).orElse(null);
            if (original != null) {
                message.setReplyToMessageId(original.getId());
                message.setReplyToContent(replyPreviewText(original));
                message.setReplyToSenderUsername(original.getSender().getUsername());
            }
        }
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

    public Message saveAudioMessage(String senderUsername, String receiverUsername, String audioUrl, Long replyToMessageId) {
        User receiver = userService.findByUsername(receiverUsername)
                .orElseThrow(() -> new RuntimeException("пользователь не найден"));
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("пользователь не найден"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("");
        message.setTimestamp(LocalDateTime.now());
        message.setAudioUrl(audioUrl);

        if (replyToMessageId != null) {
            Message original = messageRepository.findById(replyToMessageId).orElse(null);
            if (original != null) {
                message.setReplyToMessageId(original.getId());
                message.setReplyToContent(replyPreviewText(original));
                message.setReplyToSenderUsername(original.getSender().getUsername());
            }
        }

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

        String receiver = message.getReceiver().getUsername();
        if (!presenceService.isOnline(receiver)
                && !muteService.isDirectMuted(receiver, message.getSender().getUsername())) {
            pushService.sendPushToUser(
                    receiver,
                    message.getSender().getUsername(),
                    pushPreview(message),
                    "/chat/" + message.getSender().getUsername(),
                    "direct-" + message.getSender().getUsername()
            );
        }
        return dto;
    }

    private String pushPreview(Message message) {
        if (message.hasSticker()) {
            return "Стикер";
        }
        if (message.hasAudio()) {
            return "Голосовое сообщение";
        }
        String content = message.getContent();
        if (content != null && !content.isBlank()) {
            return content.length() > 120 ? content.substring(0, 120) + "…" : content;
        }
        return AttachmentService.labelForType(message.getAttachmentType());
    }

    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId).orElse(null);
    }

    public java.util.Optional<java.util.Map.Entry<String, String>> getDirectMessageUsernames(Long messageId) {
        return messageRepository.findById(messageId)
                .map(m -> Map.entry(m.getSender().getUsername(), m.getReceiver().getUsername()));
    }

    public ChatMessageDto editMessage(Long messageId, String username, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        if (!message.getSender().getUsername().equals(username)) {
            throw new RuntimeException("Только автор может редактировать сообщение");
        }
        if (newContent == null || newContent.isBlank()) {
            throw new RuntimeException("Сообщение не может быть пустым");
        }
        message.setContent(newContent.trim());
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        messageRepository.save(message);

        ChatMessageDto dto = toDto(message);
        sendAfterCommit(() -> {
            messagingTemplate.convertAndSendToUser(message.getReceiver().getUsername(), "/queue/messages", dto);
            messagingTemplate.convertAndSendToUser(message.getSender().getUsername(), "/queue/messages", dto);
        });
        return dto;
    }

    public Message forwardToUser(String sourceType, Long sourceMessageId, String targetUsername, String username) {
        String forwardedFrom = null;
        String content = null;
        String attachmentFilename = null;
        String attachmentType = null;
        String attachmentOriginalName = null;
        Long attachmentSize = null;
        String stickerCode = null;
        String stickerUrl = null;
        String audioUrl = null;

        if ("GROUP".equalsIgnoreCase(sourceType)) {
            GroupMessage source = groupMessageRepository.findById(sourceMessageId)
                    .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
            forwardedFrom = source.getSender().getUsername();
            content = source.getContent();
            attachmentFilename = source.getAttachmentFilename();
            attachmentType = source.getAttachmentType();
            attachmentOriginalName = source.getAttachmentOriginalName();
            attachmentSize = source.getAttachmentSize();
            stickerCode = source.getStickerCode();
            stickerUrl = source.getStickerUrl();
        } else {
            Message source = messageRepository.findById(sourceMessageId)
                    .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
            forwardedFrom = source.getSender().getUsername();
            content = source.getContent();
            attachmentFilename = source.getAttachmentFilename();
            attachmentType = source.getAttachmentType();
            attachmentOriginalName = source.getAttachmentOriginalName();
            attachmentSize = source.getAttachmentSize();
            stickerCode = source.getStickerCode();
            stickerUrl = source.getStickerUrl();
            audioUrl = source.getAudioUrl();
        }

        User sender = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        User receiver = userService.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("Получатель не найден"));

        Message forward = new Message();
        forward.setSender(sender);
        forward.setReceiver(receiver);
        forward.setContent(content);
        forward.setTimestamp(LocalDateTime.now());
        forward.setAttachmentFilename(attachmentFilename);
        forward.setAttachmentType(attachmentType);
        forward.setAttachmentOriginalName(attachmentOriginalName);
        forward.setAttachmentSize(attachmentSize);
        forward.setStickerCode(stickerCode);
        forward.setStickerUrl(stickerUrl);
        forward.setAudioUrl(audioUrl);
        forward.setForwardedFrom(forwardedFrom);
        return messageRepository.save(forward);
    }

    public List<Message> getChatHistory(String senderUsername,String receiverUsername) {
        return messageRepository.findChatHistory(senderUsername,receiverUsername);
    }

    public List<ChatMessageDto> getChatWindowEndingAt(String username, String partnerUsername, long anchorId, int size) {
        List<Message> messages = messageRepository.findChatWindowEndingAt(
                username, partnerUsername, anchorId, PageRequest.of(0, Math.max(1, size)));
        Collections.reverse(messages);
        return messages.stream().map(this::toDto).toList();
    }

    public void deleteConversation(String username, String partnerUsername) {
        favoriteService.removeFavoritesOfConversation(username, partnerUsername);
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
        dto.setAudioUrl(message.getAudioUrl());
        dto.setReactions(reactionService.getReactions(ReactionTargetType.DIRECT, message.getId()));
        dto.setEdited(message.isEdited());
        dto.setEditedAt(message.getEditedAt() != null
                ? message.getEditedAt().format(TIME_FORMATTER)
                : null);
        dto.setForwardedFrom(message.getForwardedFrom());
        dto.setPinned(message.isPinned());
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
            if (partner.equals(username)) {
                continue;
            }
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

    public List<Message> getPinnedDirectMessages(String username, String partnerUsername) {
        return messageRepository.findAllPinnedInConversation(username, partnerUsername);
    }

    public ChatMessageDto pinDirectMessage(Long messageId, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        partnerOf(message, username);
        message.setPinnedAt(LocalDateTime.now());
        message.setPinnedByUsername(username);
        messageRepository.save(message);
        ChatMessageDto dto = toDto(message);
        dto.setPinUpdate(true);
        sendAfterCommit(() -> broadcastPinUpdate(dto));
        return dto;
    }

    public ChatMessageDto unpinDirectMessage(Long messageId, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        partnerOf(message, username);
        message.setPinnedAt(null);
        message.setPinnedByUsername(null);
        messageRepository.save(message);
        ChatMessageDto dto = toDto(message);
        dto.setPinUpdate(true);
        sendAfterCommit(() -> broadcastPinUpdate(dto));
        return dto;
    }

    private void broadcastPinUpdate(ChatMessageDto dto) {
        messagingTemplate.convertAndSendToUser(dto.getSenderUsername(), "/queue/messages", dto);
        messagingTemplate.convertAndSendToUser(dto.getReceiverUsername(), "/queue/messages", dto);
    }

    private String partnerOf(Message message, String username) {
        if (message.getSender().getUsername().equals(username)) {
            return message.getReceiver().getUsername();
        }
        if (message.getReceiver().getUsername().equals(username)) {
            return message.getSender().getUsername();
        }
        throw new RuntimeException("Нет доступа к этому сообщению");
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
        message.setAudioUrl(null);
        message.setDeletedBySender(true);
        message.setDeletedByReceiver(true);
        message.setPinnedAt(null);
        message.setPinnedByUsername(null);
        messageRepository.save(message);
        favoriteService.removeFavoritesForMessage(FavoriteService.TYPE_DIRECT, messageId);
        ChatMessageDto dto = toDto(message);
        dto.setPinUpdate(true);
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
                : message.hasAudio() ? "Голосовое сообщение"
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
                unread,
                muteService.isDirectMuted(currentUsername, partner)
        );
    }

    public List<MessageSearchResultDto> searchDirectHistory(String username, String partnerUsername, String query, int limit) {
        String safe = sanitizeQuery(query);
        List<Message> messages = messageRepository.searchDirectChat(username, partnerUsername, safe, PageRequest.of(0, limit));
        return messages.stream().map(m -> toSearchResult(m, username)).toList();
    }

    public List<MessageSearchResultDto> searchAllDirectHistory(String username, String query, int limit) {
        String safe = sanitizeQuery(query);
        List<Message> messages = messageRepository.searchAllDirect(username, safe, PageRequest.of(0, limit));
        return messages.stream().map(m -> toSearchResult(m, username)).toList();
    }

    private MessageSearchResultDto toSearchResult(Message message, String username) {
        User partnerUser = message.getSender().getUsername().equals(username)
                ? message.getReceiver()
                : message.getSender();
        String partner = partnerUser.getUsername();

        MessageSearchResultDto dto = new MessageSearchResultDto();
        dto.setMessageId(message.getId());
        dto.setType("DIRECT");
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setPartnerUsername(partner);
        dto.setAvatarFilename(partnerUser.getAvatarFilename());
        dto.setContent(searchPreview(message));
        if (message.getTimestamp() != null) {
            dto.setTimestamp(message.getTimestamp().format(TIME_FORMATTER));
            dto.setDate(message.getTimestamp().format(DATE_FORMATTER));
            dto.setSortTimestamp(message.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        dto.setUrl("/chat/" + partner + "#msg-" + message.getId());
        return dto;
    }

    private String searchPreview(Message message) {
        if (message.hasSticker()) {
            return "Стикер";
        }
        if (message.hasAudio()) {
            return "Голосовое сообщение";
        }
        String content = message.getContent();
        if (content == null || content.isBlank()) {
            return AttachmentService.labelForType(message.getAttachmentType());
        }
        return content.length() > 200 ? content.substring(0, 200) + "…" : content;
    }

    private String sanitizeQuery(String query) {
        if (query == null) {
            return "";
        }
        String trimmed = query.trim();
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        return trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void sendAfterCommit(Runnable send) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
