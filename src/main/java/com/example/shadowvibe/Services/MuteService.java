package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.ChatMute;
import com.example.shadowvibe.Models.GroupMembership;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.ChatMuteRepository;
import com.example.shadowvibe.Repositories.GroupMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MuteService {

    private final ChatMuteRepository chatMuteRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserService userService;

    public MuteService(ChatMuteRepository chatMuteRepository,
                       GroupMembershipRepository groupMembershipRepository,
                       UserService userService) {
        this.chatMuteRepository = chatMuteRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.userService = userService;
    }

    public boolean isDirectMuted(String username, String partnerUsername) {
        if (username == null || partnerUsername == null || username.equals(partnerUsername)) {
            return false;
        }
        return chatMuteRepository.existsByUserUsernameAndPartnerUsernameAndMutedTrue(username, partnerUsername);
    }

    @Transactional
    public boolean setDirectMuted(String username, String partnerUsername, boolean muted) {
        if (partnerUsername == null || partnerUsername.equals(username)) {
            throw new IllegalArgumentException("Нельзя отключить уведомления от самого себя");
        }
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        User partner = userService.findByUsername(partnerUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь " + partnerUsername + " не найден"));

        ChatMute mute = chatMuteRepository.findByUserUsernameAndPartnerUsername(username, partnerUsername)
                .orElseGet(() -> new ChatMute(user, partner));
        mute.setMuted(muted);
        chatMuteRepository.save(mute);
        return mute.isMuted();
    }

    public boolean isGroupMuted(String username, Long groupId) {
        if (groupId == null) {
            return false;
        }
        return groupMembershipRepository.existsByGroupIdAndUserUsernameAndMutedTrue(groupId, username);
    }

    @Transactional
    public boolean setGroupMuted(String username, Long groupId, boolean muted) {
        GroupMembership membership = groupMembershipRepository.findByGroupIdAndUserUsername(groupId, username)
                .orElseThrow(() -> new IllegalArgumentException("Вы не участник этой группы"));
        membership.setMuted(muted);
        groupMembershipRepository.save(membership);
        return membership.isMuted();
    }

    public List<String> getMutedDirectPartners(String username) {
        return chatMuteRepository.findMutedPartnerUsernamesByUserUsername(username);
    }

    public List<Long> getMutedGroupIds(String username) {
        return groupMembershipRepository.findMutedGroupIdsByUserUsername(username);
    }
}
