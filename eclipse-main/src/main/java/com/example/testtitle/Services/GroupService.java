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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class GroupService {

    private final ChatGroupRepository chatGroupRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserService userService;

    public GroupService(ChatGroupRepository chatGroupRepository,
                        GroupMessageRepository groupMessageRepository,
                        GroupMembershipRepository groupMembershipRepository,
                        UserService userService) {
        this.chatGroupRepository = chatGroupRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.userService = userService;
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

        List<GroupPreviewDto> previews = new ArrayList<>();
        for (ChatGroup group : groups) {
            GroupMembership membership = groupMembershipRepository
                    .findByGroupIdAndUserUsername(group.getId(), username)
                    .orElse(null);
            LocalDateTime lastReadAt = membership != null ? membership.getLastReadAt() : null;
            long unread = groupMessageRepository.countUnreadForMember(group.getId(), username, lastReadAt);

            GroupMessage latest = latestByGroup.get(group.getId());
            String preview = latest != null ? truncate(latest.getContent()) : "Нет сообщений";
            String time = latest != null && latest.getTimestamp() != null
                    ? latest.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"))
                    : "";
            String sender = latest != null ? latest.getSender().getUsername() : "";

            previews.add(new GroupPreviewDto(
                    group.getId(),
                    group.getName(),
                    preview,
                    time,
                    sender,
                    unread
            ));
        }
        return previews;
    }

    public List<GroupMessage> getGroupHistory(Long groupId, String username) {
        getGroupForMember(groupId, username);
        return groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId);
    }

    public GroupMessage saveGroupMessage(String senderUsername, Long groupId, String content) {
        ChatGroup group = getGroupForMember(groupId, senderUsername);
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        GroupMessage message = new GroupMessage();
        message.setContent(content);
        message.setSender(sender);
        message.setGroup(group);
        message.setTimestamp(LocalDateTime.now());
        return groupMessageRepository.save(message);
    }

    public void markGroupAsRead(String username, Long groupId) {
        GroupMembership membership = groupMembershipRepository
                .findByGroupIdAndUserUsername(groupId, username)
                .orElseThrow(() -> new RuntimeException("Участник группы не найден"));
        membership.setLastReadAt(LocalDateTime.now());
        groupMembershipRepository.save(membership);
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
                time
        );
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 48 ? text.substring(0, 48) + "…" : text;
    }
}
