package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.ChatGroup;
import com.example.shadowvibe.Models.GroupInvite;
import com.example.shadowvibe.Models.GroupMembership;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.ChatGroupRepository;
import com.example.shadowvibe.Repositories.GroupInviteRepository;
import com.example.shadowvibe.Repositories.GroupMembershipRepository;
import com.example.shadowvibe.Repositories.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class GroupInviteService {

    private final GroupInviteRepository inviteRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public GroupInviteService(GroupInviteRepository inviteRepository,
                              ChatGroupRepository chatGroupRepository,
                              GroupMembershipRepository groupMembershipRepository,
                              UserRepository userRepository,
                              SimpMessagingTemplate messagingTemplate) {
        this.inviteRepository = inviteRepository;
        this.chatGroupRepository = chatGroupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public List<GroupInvite> getPendingInvites(String username) {
        return inviteRepository.findByInvitedUser_UsernameAndStatusOrderByCreatedAtDesc(
                username, GroupInvite.Status.PENDING);
    }

    public long getPendingCount(String username) {
        return inviteRepository.countByInvitedUser_UsernameAndStatus(
                username, GroupInvite.Status.PENDING);
    }

    public record InviteResult(List<GroupInvite> created, List<String> alreadyMembers, List<String> notFound) {}

    public InviteResult createInvites(Long groupId, String inviterUsername, List<String> usernames) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        User inviter = userRepository.findByUsername(inviterUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (!group.isMember(inviterUsername)) {
            throw new IllegalArgumentException("Вы не являетесь участником этой группы");
        }

        List<GroupInvite> created = new ArrayList<>();
        List<String> alreadyMembers = new ArrayList<>();
        List<String> notFound = new ArrayList<>();

        for (String username : usernames) {
            if (username.equals(inviterUsername)) continue;
            if (group.isMember(username)) {
                alreadyMembers.add(username);
                continue;
            }

            User target = userRepository.findByUsername(username).orElse(null);
            if (target == null) {
                notFound.add(username);
                continue;
            }

            var existing = inviteRepository.findByGroupIdAndInvitedUser_UsernameAndStatus(
                    groupId, username, GroupInvite.Status.PENDING);
            if (existing.isPresent()) continue;

            var existingAccepted = inviteRepository.findByGroupIdAndInvitedUser_UsernameAndStatus(
                    groupId, username, GroupInvite.Status.ACCEPTED);
            if (existingAccepted.isPresent()) continue;

            GroupInvite invite = new GroupInvite(group, inviter, target);
            inviteRepository.save(invite);
            created.add(invite);

            sendInviteNotification(target.getUsername(), invite, "created");
        }
        return new InviteResult(created, alreadyMembers, notFound);
    }

    public void acceptInvite(Long inviteId, String username) {
        GroupInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Приглашение не найдено"));

        if (!invite.getInvitedUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Это приглашение не для вас");
        }
        if (invite.getStatus() != GroupInvite.Status.PENDING) {
            throw new IllegalArgumentException("Приглашение уже обработано");
        }

        invite.setStatus(GroupInvite.Status.ACCEPTED);
        inviteRepository.save(invite);

        ChatGroup group = invite.getGroup();
        if (!group.isMember(username)) {
            group.getMembers().add(invite.getInvitedUser());
            chatGroupRepository.save(group);
            groupMembershipRepository.save(new GroupMembership(group, invite.getInvitedUser()));
        }

        sendInviteNotification(username, invite, "accepted");
        sendInviteNotification(invite.getInvitedBy().getUsername(), invite, "accepted");
    }

    public void declineInvite(Long inviteId, String username) {
        GroupInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Приглашение не найдено"));

        if (!invite.getInvitedUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Это приглашение не для вас");
        }
        if (invite.getStatus() != GroupInvite.Status.PENDING) {
            throw new IllegalArgumentException("Приглашение уже обработано");
        }

        invite.setStatus(GroupInvite.Status.DECLINED);
        inviteRepository.save(invite);

        sendInviteNotification(invite.getInvitedBy().getUsername(), invite, "declined");
    }

    private void sendInviteNotification(String targetUsername, GroupInvite invite, String action) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("type", "group_invite");
        dto.put("action", action);
        dto.put("inviteId", invite.getId());
        dto.put("groupId", invite.getGroup().getId());
        dto.put("groupName", invite.getGroup().getName());
        dto.put("invitedBy", invite.getInvitedBy().getUsername());
        dto.put("invitedUser", invite.getInvitedUser().getUsername());
        dto.put("status", invite.getStatus().name());
        messagingTemplate.convertAndSendToUser(targetUsername, "/queue/group-invites", dto);
    }
}
