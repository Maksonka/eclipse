package com.example.testtitle.Services;

import com.example.testtitle.DTO.GroupMessageDto;
import com.example.testtitle.DTO.GroupPreviewDto;
import com.example.testtitle.Models.ChatGroup;
import com.example.testtitle.Models.GroupMembership;
import com.example.testtitle.Models.GroupMessage;
import com.example.testtitle.Models.User;
import com.example.testtitle.Repositories.ChatGroupRepository;
import com.example.testtitle.Repositories.GroupMembershipRepository;
import com.example.testtitle.Repositories.GroupMessageRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
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

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final ChatGroupRepository chatGroupRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AttachmentService attachmentService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public GroupService(ChatGroupRepository chatGroupRepository,
                        GroupMessageRepository groupMessageRepository,
                        GroupMembershipRepository groupMembershipRepository,
                        UserService userService,
                        SimpMessagingTemplate messagingTemplate,
                        AttachmentService attachmentService) {
        this.chatGroupRepository = chatGroupRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
        this.attachmentService = attachmentService;
    }

    public ChatGroup createGroup(String creatorUsername, String name, String memberUsernames) {
        User creator = userService.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        ChatGroup group = new ChatGroup(name.trim(), creator);
        Set<User> members = new HashSet<>();
        members.add(creator);

        if (memberUsernames != null && !memberUsernames.isBlank()) {
            Set<String> usernames = Arrays.stream(memberUsernames.split("[,;\\s]+"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .filter(s -> !s.equalsIgnoreCase(creatorUsername))
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

            for (String username : usernames) {
                User member = userService.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + username));
                members.add(member);
            }
        }

        group.setMembers(members);
        ChatGroup saved = chatGroupRepository.save(group);

        for (User member : members) {
            groupMembershipRepository.save(new GroupMembership(saved, member));
        }

        return saved;
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
                    ? (latest.getContent() != null && !latest.getContent().isBlank()
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
                    unreadByGroup.getOrDefault(group.getId(), 0L)
            ));
        }
        return previews;
    }

    public List<GroupMessage> getGroupHistory(Long groupId, String username) {
        getGroupForMember(groupId, username);
        return groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId);
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
        AttachmentService.AttachmentInfo info = attachmentService.save(file);

        ChatGroup group = getGroupForMember(groupId, senderUsername);
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        GroupMessage message = new GroupMessage();
        message.setContent("");
        message.setSender(sender);
        message.setGroup(group);
        message.setTimestamp(LocalDateTime.now());
        message.setAttachmentFilename(info.filename());
        message.setAttachmentType(info.type());
        message.setAttachmentOriginalName(info.originalName());
        message.setAttachmentSize(info.size());
        return groupMessageRepository.save(message);
    }

    public GroupMessageDto broadcastGroupMessage(GroupMessage message) {
        GroupMessageDto dto = toDto(message);
        messagingTemplate.convertAndSend("/topic/group." + message.getGroup().getId(), dto);
        return dto;
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
        return new GroupMessageDto(
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
        groupMessageRepository.save(message);
        return toDto(message);
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 48 ? text.substring(0, 48) + "…" : text;
    }
}
