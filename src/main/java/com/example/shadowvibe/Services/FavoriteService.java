package com.example.shadowvibe.Services;

import com.example.shadowvibe.DTO.FavoriteMessageDto;
import com.example.shadowvibe.Models.FavoriteMessage;
import com.example.shadowvibe.Models.GroupMessage;
import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.ChatGroupRepository;
import com.example.shadowvibe.Repositories.FavoriteMessageRepository;
import com.example.shadowvibe.Repositories.GroupMessageRepository;
import com.example.shadowvibe.Repositories.MessageRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class FavoriteService {

    public static final String TYPE_DIRECT = "DIRECT";
    public static final String TYPE_GROUP = "GROUP";

    private static final DateTimeFormatter FAVORITED_AT_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final FavoriteMessageRepository favoriteMessageRepository;
    private final MessageRepository messageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final UserService userService;
    // private final PremiumService premiumService; // PREMIUM отключён

    public FavoriteService(FavoriteMessageRepository favoriteMessageRepository,
                           MessageRepository messageRepository,
                           GroupMessageRepository groupMessageRepository,
                           ChatGroupRepository chatGroupRepository,
                           UserService userService) {
        this.favoriteMessageRepository = favoriteMessageRepository;
        this.messageRepository = messageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.chatGroupRepository = chatGroupRepository;
        this.userService = userService;
    }

    public FavoriteMessageDto toggleDirect(Long messageId, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        partnerOf(message, username);
        return toggle(username, TYPE_DIRECT, messageId);
    }

    public FavoriteMessageDto toggleGroup(Long messageId, String username) {
        GroupMessage message = groupMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        Set<Long> groupIds = chatGroupRepository.findAllByMemberUsername(username).stream()
                .map(group -> group.getId())
                .collect(Collectors.toSet());
        if (!groupIds.contains(message.getGroup().getId())) {
            throw new RuntimeException("Нет доступа к этой группе");
        }
        return toggle(username, TYPE_GROUP, messageId);
    }

    private FavoriteMessageDto toggle(String username, String targetType, Long messageId) {
        FavoriteMessageDto dto = new FavoriteMessageDto();
        dto.setType(targetType);
        dto.setMessageId(messageId);

        var existing = favoriteMessageRepository.findByUser_UsernameAndTargetTypeAndMessageId(
                username, targetType, messageId);
        if (existing.isPresent()) {
            favoriteMessageRepository.delete(existing.get());
            dto.setFavorited(false);
        } else {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
            favoriteMessageRepository.save(new FavoriteMessage(user, targetType, messageId));
            dto.setFavorited(true);
        }
        return dto;
    }

    public List<FavoriteMessageDto> getFavorites(String username) {
        List<FavoriteMessageDto> result = new ArrayList<>();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        long currentUserId = currentUser.getId();

        for (FavoriteMessage favorite : favoriteMessageRepository.findAllByUser_UsernameOrderByFavoritedAtDesc(username)) {
            if (TYPE_DIRECT.equals(favorite.getTargetType())) {
                Message message = messageRepository.findById(favorite.getMessageId()).orElse(null);
                if (message == null || isDirectDeletedForMe(message, username)) {
                    continue;
                }
                User partner = message.getSender().getUsername().equals(username)
                        ? message.getReceiver()
                        : message.getSender();
                FavoriteMessageDto dto = new FavoriteMessageDto();
                dto.setType(TYPE_DIRECT);
                dto.setMessageId(message.getId());
                dto.setSenderUsername(message.getSender().getUsername());
                dto.setPreview(previewText(
                        message.getContent(),
                        message.getStickerUrl(),
                        message.getAudioUrl(),
                        message.getAttachmentType()));
                dto.setChatTitle(partner.getUsername());
                dto.setChatAvatarFilename(partner.getAvatarFilename());
                dto.setChatHref("/chat/" + partner.getUsername());
                dto.setAttachmentType(message.getAttachmentType());
                dto.setAttachmentFilename(message.getAttachmentFilename());
                fillTime(dto, favorite);
                result.add(dto);
            } else {
                GroupMessage message = groupMessageRepository.findById(favorite.getMessageId()).orElse(null);
                if (message == null || message.isDeletedByUser(currentUserId)) {
                    continue;
                }
                FavoriteMessageDto dto = new FavoriteMessageDto();
                dto.setType(TYPE_GROUP);
                dto.setMessageId(message.getId());
                dto.setSenderUsername(message.getSender().getUsername());
                dto.setPreview(previewText(
                        message.getContent(),
                        message.getStickerUrl(),
                        null,
                        message.getAttachmentType()));
                dto.setChatTitle(message.getGroup().getName());
                dto.setChatAvatarFilename(message.getGroup().getAvatarFilename());
                dto.setChatHref("/chat/group/" + message.getGroup().getId());
                dto.setAttachmentType(message.getAttachmentType());
                dto.setAttachmentFilename(message.getAttachmentFilename());
                fillTime(dto, favorite);
                result.add(dto);
            }
        }
        return result;
    }

    public List<Long> getFavoritedIds(String username, String type, String partner, Long groupId) {
        if (TYPE_DIRECT.equals(type)) {
            if (partner == null || partner.isBlank()) {
                return List.of();
            }
            return favoriteMessageRepository.findFavoritedIdsInDirectConversation(username, partner);
        }
        if (TYPE_GROUP.equals(type)) {
            if (groupId == null) {
                return List.of();
            }
            return favoriteMessageRepository.findFavoritedIdsInGroup(username, groupId);
        }
        return List.of();
    }

    public long count(String username) {
        return favoriteMessageRepository.countByUser_Username(username);
    }

    public void removeFavoritesForMessage(String targetType, Long messageId) {
        favoriteMessageRepository.deleteAllByTargetTypeAndMessageId(targetType, messageId);
    }

    public void removeFavoritesOfConversation(String username, String partnerUsername) {
        favoriteMessageRepository.deleteFavoritesOfConversation(username, partnerUsername);
    }

    public void removeFavoritesOfGroup(Long groupId) {
        favoriteMessageRepository.deleteFavoritesOfGroup(groupId);
    }

    private void fillTime(FavoriteMessageDto dto, FavoriteMessage favorite) {
        if (favorite.getFavoritedAt() != null) {
            dto.setFavoritedAt(favorite.getFavoritedAt().format(FAVORITED_AT_FORMATTER));
            dto.setSortTimestamp(favorite.getFavoritedAt()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
    }

    private String previewText(String content, String stickerUrl, String audioUrl, String attachmentType) {
        if (stickerUrl != null && !stickerUrl.isBlank()) {
            return "Стикер";
        }
        if (audioUrl != null && !audioUrl.isBlank()) {
            return "Голосовое сообщение";
        }
        if (content != null && !content.isBlank()) {
            return content.length() > 200 ? content.substring(0, 200) + "…" : content;
        }
        return AttachmentService.labelForType(attachmentType);
    }

    private boolean isDirectDeletedForMe(Message message, String username) {
        if (message.getSender().getUsername().equals(username)) {
            return message.isDeletedBySender();
        }
        return message.isDeletedByReceiver();
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
}
