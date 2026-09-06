package com.example.shadowvibe.Services;

import com.example.shadowvibe.DTO.GroupMessageDto;
import com.example.shadowvibe.DTO.GroupPreviewDto;
import com.example.shadowvibe.Models.ChatGroup;
import com.example.shadowvibe.Models.GroupMembership;
import com.example.shadowvibe.Models.GroupMessage;
import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.Sticker;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.ChatGroupRepository;
import com.example.shadowvibe.Repositories.GroupMembershipRepository;
import com.example.shadowvibe.Repositories.GroupMessageRepository;
import com.example.shadowvibe.Repositories.MessageRepository;
import com.example.shadowvibe.enums.ReactionTargetType;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class GroupService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final ChatGroupRepository chatGroupRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AttachmentService attachmentService;
    private final StickerService stickerService;
    private final ReactionService reactionService;
    private final PushService pushService;
    private final PresenceService presenceService;
    private final MuteService muteService;
    private final FavoriteService favoriteService;
    // private final PremiumService premiumService; // PREMIUM отключён

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public GroupService(ChatGroupRepository chatGroupRepository,
                        GroupMessageRepository groupMessageRepository,
                        GroupMembershipRepository groupMembershipRepository,
                        MessageRepository messageRepository,
                        UserService userService,
                        SimpMessagingTemplate messagingTemplate,
                        AttachmentService attachmentService,
                        StickerService stickerService,
                        ReactionService reactionService,
        PushService pushService,
        PresenceService presenceService,
        MuteService muteService,
        FavoriteService favoriteService) {
        this.chatGroupRepository = chatGroupRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
        this.attachmentService = attachmentService;
        this.stickerService = stickerService;
        this.reactionService = reactionService;
        this.pushService = pushService;
        this.presenceService = presenceService;
        this.muteService = muteService;
        this.favoriteService = favoriteService;
        // this.premiumService = premiumService; // PREMIUM отключён
    }

    public ChatGroup createGroup(String creatorUsername, String name, String memberUsernames) {
        User creator = userService.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        ChatGroup group = new ChatGroup(name.trim(), creator);
        Set<User> members = new HashSet<>();
        members.add(creator);

        Set<String> inviteTargetUsernames = new java.util.LinkedHashSet<>();
        if (memberUsernames != null && !memberUsernames.isBlank()) {
            inviteTargetUsernames = Arrays.stream(memberUsernames.split("[,;\\s]+"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .filter(s -> !s.equalsIgnoreCase(creatorUsername))
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        }

        group.setMembers(members);
        ChatGroup saved = chatGroupRepository.save(group);
        groupMembershipRepository.save(new GroupMembership(saved, creator));

        return saved;
    }

    public java.util.List<String> getInviteTargetUsernames(String memberUsernames, String creatorUsername) {
        if (memberUsernames == null || memberUsernames.isBlank()) {
            return List.of();
        }
        return Arrays.stream(memberUsernames.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> !s.equalsIgnoreCase(creatorUsername))
                .toList();
    }

    public ChatGroup getGroupForMember(Long groupId, String username) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));
        if (!group.isMember(username)) {
            throw new RuntimeException("Нет доступа к группе");
        }
        return group;
    }

    public List<GroupPreviewDto> getGroupPreviews(String username) {
        List<ChatGroup> groups = chatGroupRepository.findAllByMemberUsername(username);
        if (groups.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = groups.stream().map(ChatGroup::getId).toList();
        Map<Long, GroupMessage> latestByGroup = new LinkedHashMap<>();
        for (GroupMessage message : groupMessageRepository.findRecentByGroupIds(groupIds)) {
            latestByGroup.putIfAbsent(message.getGroup().getId(), message);
        }

        Map<Long, Long> unreadByGroup = new HashMap<>();
        for (Object[] row : groupMessageRepository.countUnreadByGroupsForUser(groupIds, username)) {
            unreadByGroup.put((Long) row[0], (Long) row[1]);
        }

        List<GroupPreviewDto> previews = new ArrayList<>();
        for (ChatGroup group : groups) {
            GroupMessage latest = latestByGroup.get(group.getId());
            String preview = latest != null
                    ? (latest.hasSticker() ? "Стикер"
                        : latest.hasAudio() ? "Голосовое сообщение"
                        : latest.getContent() != null && !latest.getContent().isBlank()
                        ? truncate(latest.getContent())
                        : AttachmentService.labelForType(latest.getAttachmentType()))
                    : "Нет сообщений";
            String time = latest != null && latest.getTimestamp() != null
                    ? latest.getTimestamp().format(TIME_FORMATTER)
                    : "";
            String sender = latest != null ? latest.getSender().getUsername() : "";

            previews.add(new GroupPreviewDto(
                    group.getId(),
                    group.getName(),
                    group.getAvatarFilename(),
                    preview,
                    time,
                    sender,
                    unreadByGroup.getOrDefault(group.getId(), 0L),
                    muteService.isGroupMuted(username, group.getId())
            ));
        }
        return previews;
    }

    public List<GroupMessage> getGroupHistory(Long groupId, String username) {
        getGroupForMember(groupId, username);
        return groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId);
    }

    public List<GroupMessageDto> getGroupWindowEndingAt(String username, Long groupId, long anchorId, int size) {
        getGroupForMember(groupId, username);
        List<GroupMessage> messages = groupMessageRepository.findGroupWindowEndingAt(
                groupId, anchorId, PageRequest.of(0, Math.max(1, size)));
        Collections.reverse(messages);
        return messages.stream().map(this::toDto).toList();
    }

    public GroupMessage saveGroupMessage(String senderUsername, Long groupId, String content, Long replyToMessageId) {
        ChatGroup group = getGroupForMember(groupId, senderUsername);
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        GroupMessage message = new GroupMessage();
        message.setContent(content);
        message.setSender(sender);
        message.setGroup(group);
        message.setTimestamp(LocalDateTime.now());

        if (replyToMessageId != null) {
            GroupMessage original = groupMessageRepository.findById(replyToMessageId).orElse(null);
            if (original != null) {
                message.setReplyToMessageId(original.getId());
                message.setReplyToContent(original.getContent() != null ? original.getContent() : "");
                message.setReplyToSenderUsername(original.getSender().getUsername());
            }
        }

        return groupMessageRepository.save(message);
    }

    public GroupMessage saveGroupAttachmentMessage(String senderUsername, Long groupId, MultipartFile file) throws IOException {
        return saveGroupAttachmentMessage(senderUsername, groupId, file, null, null);
    }

    public GroupMessage saveGroupAttachmentMessage(String senderUsername, Long groupId, MultipartFile file,
                                                   String content, Long replyToMessageId) throws IOException {
        AttachmentService.AttachmentInfo info = attachmentService.save(file);

        ChatGroup group = getGroupForMember(groupId, senderUsername);
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        GroupMessage message = new GroupMessage();
        message.setContent(content == null ? "" : content.trim());
        message.setSender(sender);
        message.setGroup(group);
        message.setTimestamp(LocalDateTime.now());
        message.setAttachmentFilename(info.filename());
        message.setAttachmentType(info.type());
        message.setAttachmentOriginalName(info.originalName());
        message.setAttachmentSize(info.size());

        if (replyToMessageId != null) {
            GroupMessage original = groupMessageRepository.findById(replyToMessageId).orElse(null);
            if (original != null) {
                message.setReplyToMessageId(original.getId());
                message.setReplyToContent(original.getContent() != null ? original.getContent() : "");
                message.setReplyToSenderUsername(original.getSender().getUsername());
            }
        }
        return groupMessageRepository.save(message);
    }

    public GroupMessage saveGroupAudioMessage(String senderUsername, Long groupId,
                                              String audioUrl, Long audioDurationMs,
                                              Long replyToMessageId) {
        ChatGroup group = getGroupForMember(groupId, senderUsername);
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        GroupMessage message = new GroupMessage();
        message.setContent("");
        message.setSender(sender);
        message.setGroup(group);
        message.setTimestamp(LocalDateTime.now());
        message.setAudioUrl(audioUrl);
        message.setAudioDurationMs(audioDurationMs);

        if (replyToMessageId != null) {
            GroupMessage original = groupMessageRepository.findById(replyToMessageId).orElse(null);
            if (original != null) {
                message.setReplyToMessageId(original.getId());
                message.setReplyToContent(original.getContent() != null ? original.getContent() : "");
                message.setReplyToSenderUsername(original.getSender().getUsername());
            }
        }

        return groupMessageRepository.save(message);
    }

    public GroupMessage saveGroupStickerMessage(String senderUsername, Long groupId, String stickerCode) {
        Sticker sticker = stickerService.findByCode(stickerCode);
        if (sticker == null) {
            throw new RuntimeException("Стикер не найден");
        }

        ChatGroup group = getGroupForMember(groupId, senderUsername);
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        GroupMessage message = new GroupMessage();
        message.setContent("");
        message.setSender(sender);
        message.setGroup(group);
        message.setTimestamp(LocalDateTime.now());
        message.setStickerCode(sticker.getCode());
        message.setStickerUrl(sticker.getFilename());
        return groupMessageRepository.save(message);
    }

    public GroupMessageDto broadcastGroupMessage(GroupMessage message) {
        GroupMessageDto dto = toDto(message);
        messagingTemplate.convertAndSend("/topic/group." + message.getGroup().getId(), dto);
        sendGroupPush(message);
        return dto;
    }

    private void sendGroupPush(GroupMessage message) {
        String sender = message.getSender().getUsername();
        ChatGroup group = message.getGroup();
        String groupName = group.getName();
        String preview = message.hasSticker() ? "Стикер"
                : (message.getContent() != null && !message.getContent().isBlank()
                        ? message.getContent()
                        : AttachmentService.labelForType(message.getAttachmentType()));
        if (preview.length() > 120) {
            preview = preview.substring(0, 120) + "…";
        }
        String body = sender + ": " + preview;

        for (User member : group.getMembers()) {
            String username = member.getUsername();
            if (username.equals(sender)
                    || presenceService.isOnline(username)
                    || muteService.isGroupMuted(username, group.getId())) {
                continue;
            }
            pushService.sendPushToUser(
                    username,
                    groupName,
                    body,
                    "/chat/group/" + group.getId(),
                    "group-" + group.getId()
            );
        }
    }

    public void markGroupAsRead(String username, Long groupId) {
        GroupMembership membership = groupMembershipRepository
                .findByGroupIdAndUserUsername(groupId, username)
                .orElseThrow(() -> new RuntimeException("Участник группы не найден"));
        membership.setLastReadAt(LocalDateTime.now());
        groupMembershipRepository.save(membership);
    }

    public void removeMember(Long groupId, String creatorUsername, String memberUsername) {
        ChatGroup group = getGroupForCreator(groupId, creatorUsername);
        if (memberUsername == null || memberUsername.equals(creatorUsername)) {
            throw new IllegalArgumentException("Нельзя удалить создателя группы");
        }
        if (!group.isMember(memberUsername)) {
            throw new IllegalArgumentException("Пользователь не является участником группы");
        }
        group.getMembers().removeIf(m -> m.getUsername().equals(memberUsername));
        chatGroupRepository.save(group);
        groupMembershipRepository.deleteByGroupIdAndUserUsername(groupId, memberUsername);
    }

    public void leaveGroup(Long groupId, String username) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));
        if (group.getCreatedBy().getUsername().equals(username)) {
            throw new IllegalArgumentException("Создатель не может покинуть группу. Удалите группу вместо этого");
        }
        if (!group.isMember(username)) {
            throw new IllegalArgumentException("Вы не являетесь участником группы");
        }
        group.getMembers().removeIf(m -> m.getUsername().equals(username));
        chatGroupRepository.save(group);
        groupMembershipRepository.deleteByGroupIdAndUserUsername(groupId, username);
    }

    public void addMembers(Long groupId, String creatorUsername, String memberUsernames) {
        ChatGroup group = getGroupForCreator(groupId, creatorUsername);
        if (memberUsernames == null || memberUsernames.isBlank()) {
            throw new IllegalArgumentException("Укажите хотя бы одного участника");
        }

        Set<String> usernames = Arrays.stream(memberUsernames.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> !group.isMember(s))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        if (usernames.isEmpty()) {
            throw new IllegalArgumentException("Все указанные пользователи уже являются участниками группы");
        }

        for (String username : usernames) {
            User member = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + username));
            group.getMembers().add(member);
            groupMembershipRepository.save(new GroupMembership(group, member));
        }
        chatGroupRepository.save(group);
    }

    public void deleteGroup(Long groupId, String creatorUsername) {
        ChatGroup group = getGroupForCreator(groupId, creatorUsername);
        try {
            deleteGroupAvatarFile(group.getAvatarFilename());
        } catch (IOException e) {
            throw new RuntimeException("Не удалось удалить файл аватара группы", e);
        }
        groupMembershipRepository.deleteByGroupId(groupId);
        groupMessageRepository.deleteByGroupId(groupId);
        favoriteService.removeFavoritesOfGroup(groupId);
        chatGroupRepository.delete(group);
    }

    private ChatGroup getGroupForCreator(Long groupId, String creatorUsername) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));
        if (!group.getCreatedBy().getUsername().equals(creatorUsername)) {
            throw new IllegalArgumentException("Это может сделать только создатель группы");
        }
        return group;
    }

    public List<User> getGroupMembers(Long groupId, String username) {
        ChatGroup group = getGroupForMember(groupId, username);
        return group.getMembers().stream()
                .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
                .toList();
    }

    public ChatGroup updateGroupAvatar(Long groupId, String username, MultipartFile avatarFile) throws IOException {
        ChatGroup group = getGroupForMember(groupId, username);
        if (!group.getCreatedBy().getUsername().equals(username)) {
            throw new IllegalArgumentException("Изменить аватар может только создатель группы");
        }

        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new IllegalArgumentException("Выберите файл изображения");
        }
        String contentType = avatarFile.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Допустимы только изображения JPG, PNG, WEBP или GIF");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };

        deleteGroupAvatarFile(group.getAvatarFilename());

        String filename = "group_" + group.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path avatarDir = Paths.get(uploadDir, "group-avatars").toAbsolutePath().normalize();
        Files.createDirectories(avatarDir);

        Path target = avatarDir.resolve(filename);
        Files.copy(avatarFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        group.setAvatarFilename(filename);
        return chatGroupRepository.save(group);
    }

    private void deleteGroupAvatarFile(String filename) throws IOException {
        if (filename == null || filename.isBlank()) {
            return;
        }
        Path file = Paths.get(uploadDir, "group-avatars", filename).toAbsolutePath().normalize();
        Files.deleteIfExists(file);
    }

    public GroupMessageDto toDto(GroupMessage message) {
        String time = message.getTimestamp() != null
                ? message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "";
        GroupMessageDto dto = new GroupMessageDto(
                message.getId(),
                message.getGroup().getId(),
                message.getContent(),
                message.getSender().getUsername(),
                time,
                message.getAttachmentFilename(),
                message.getAttachmentType(),
                message.getAttachmentOriginalName(),
                message.getAttachmentSize(),
                message.getReplyToMessageId(),
                message.getReplyToContent(),
                message.getReplyToSenderUsername(),
                message.getDeletedByUserIds()
        );
        dto.setStickerCode(message.getStickerCode());
        dto.setStickerUrl(message.getStickerUrl());
        dto.setAudioUrl(message.getAudioUrl());
        dto.setAudioDurationMs(message.getAudioDurationMs());
        dto.setReactions(reactionService.getReactions(ReactionTargetType.GROUP, message.getId()));
        dto.setEdited(message.isEdited());
        dto.setEditedAt(message.getEditedAt() != null
                ? message.getEditedAt().format(DateTimeFormatter.ofPattern("HH:mm"))
                : null);
        dto.setForwardedFrom(message.getForwardedFrom());
        dto.setPinned(message.isPinned());
        return dto;
    }

    public GroupMessageDto deleteGroupMessageForMe(Long messageId, String username) {
        GroupMessage message = groupMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        message.addDeletion(user.getId());
        groupMessageRepository.save(message);
        return toDto(message);
    }

    public GroupMessageDto deleteGroupMessageForEveryone(Long messageId, String username) {
        GroupMessage message = groupMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        if (!message.getSender().getUsername().equals(username)) {
            throw new RuntimeException("Только автор может удалить сообщение для всех");
        }
        for (User member : message.getGroup().getMembers()) {
            message.addDeletion(member.getId());
        }
        message.setContent("Сообщение удалено");
        message.setAttachmentFilename(null);
        message.setAttachmentType(null);
        message.setAttachmentOriginalName(null);
        message.setAttachmentSize(null);
        message.setStickerCode(null);
        message.setStickerUrl(null);
        message.setPinnedAt(null);
        message.setPinnedByUsername(null);
        groupMessageRepository.save(message);
        favoriteService.removeFavoritesForMessage(FavoriteService.TYPE_GROUP, messageId);
        GroupMessageDto dto = toDto(message);
        dto.setPinUpdate(true);
        return dto;
    }

    public List<GroupMessage> getPinnedGroupMessages(Long groupId, String username) {
        getGroupForMember(groupId, username);
        return groupMessageRepository.findAllPinnedInGroup(groupId);
    }

    public GroupMessageDto pinGroupMessage(Long messageId, Long groupId, String username) {
        getGroupForMember(groupId, username);
        GroupMessage message = groupMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        if (!message.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Сообщение не принадлежит этой группе");
        }
        message.setPinnedAt(LocalDateTime.now());
        message.setPinnedByUsername(username);
        groupMessageRepository.save(message);
        GroupMessageDto dto = toDto(message);
        dto.setPinUpdate(true);
        sendAfterCommit(() -> messagingTemplate.convertAndSend("/topic/group." + groupId, dto));
        return dto;
    }

    public GroupMessageDto unpinGroupMessage(Long messageId, Long groupId, String username) {
        getGroupForMember(groupId, username);
        GroupMessage message = groupMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        if (!message.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Сообщение не принадлежит этой группе");
        }
        message.setPinnedAt(null);
        message.setPinnedByUsername(null);
        groupMessageRepository.save(message);
        GroupMessageDto dto = toDto(message);
        dto.setPinUpdate(true);
        sendAfterCommit(() -> messagingTemplate.convertAndSend("/topic/group." + groupId, dto));
        return dto;
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 48 ? text.substring(0, 48) + "…" : text;
    }

    public GroupMessageDto editGroupMessage(Long messageId, Long groupId, String username, String newContent) {
        GroupMessage message = groupMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
        getGroupForMember(groupId, username);
        if (!message.getSender().getUsername().equals(username)) {
            throw new RuntimeException("Только автор может редактировать сообщение");
        }
        if (newContent == null || newContent.isBlank()) {
            throw new RuntimeException("Сообщение не может быть пустым");
        }
        message.setContent(newContent.trim());
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        groupMessageRepository.save(message);
        GroupMessageDto dto = toDto(message);
        sendAfterCommit(() -> messagingTemplate.convertAndSend("/topic/group." + groupId, dto));
        return dto;
    }

    public GroupMessage forwardToGroup(String sourceType, Long sourceMessageId, Long groupId, String username) {
        String forwardedFrom = null;
        String content = null;
        String attachmentFilename = null;
        String attachmentType = null;
        String attachmentOriginalName = null;
        Long attachmentSize = null;
        String stickerCode = null;
        String stickerUrl = null;

        if ("DIRECT".equalsIgnoreCase(sourceType)) {
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
            if (source.hasAudio()) {
                content = "Голосовое сообщение";
                attachmentFilename = null;
                attachmentType = null;
                attachmentOriginalName = null;
                attachmentSize = null;
                stickerCode = null;
                stickerUrl = null;
            }
        } else {
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
        }

        ChatGroup group = getGroupForMember(groupId, username);
        User sender = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        GroupMessage forward = new GroupMessage();
        forward.setContent(content);
        forward.setSender(sender);
        forward.setGroup(group);
        forward.setTimestamp(LocalDateTime.now());
        forward.setAttachmentFilename(attachmentFilename);
        forward.setAttachmentType(attachmentType);
        forward.setAttachmentOriginalName(attachmentOriginalName);
        forward.setAttachmentSize(attachmentSize);
        forward.setStickerCode(stickerCode);
        forward.setStickerUrl(stickerUrl);
        forward.setForwardedFrom(forwardedFrom);
        return groupMessageRepository.save(forward);
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
