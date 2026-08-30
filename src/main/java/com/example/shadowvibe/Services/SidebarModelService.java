package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.User;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.security.Principal;
import java.util.Map;

@Service
public class SidebarModelService {

    private final UserService userService;
    private final MessageService messageService;
    private final GroupService groupService;
    private final PresenceService presenceService;
    private final MuteService muteService;

    public SidebarModelService(UserService userService,
                               MessageService messageService,
                               GroupService groupService,
                               PresenceService presenceService,
                               MuteService muteService) {
        this.userService = userService;
        this.messageService = messageService;
        this.groupService = groupService;
        this.presenceService = presenceService;
        this.muteService = muteService;
    }

    public void populate(Model model, Principal principal,
                         String activeUsername, Long activeGroupId, String searchQuery) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUsername", principal.getName());
        model.addAttribute("activeUsername", activeUsername);
        model.addAttribute("activeGroupId", activeGroupId);

        Map<String, Long> unreadByPartner = messageService.getUnreadCountsByPartner(principal.getName());
        model.addAttribute("conversations", messageService.getConversations(principal.getName(), unreadByPartner));
        model.addAttribute("groups", groupService.getGroupPreviews(principal.getName()));
        model.addAttribute("onlineUsers", presenceService.getOnlinePartners(principal.getName()));
        model.addAttribute("totalUnread", messageService.getTotalUnreadCount(unreadByPartner));
        model.addAttribute("mutedDirectPartners", muteService.getMutedDirectPartners(principal.getName()));
        model.addAttribute("mutedGroupIds", muteService.getMutedGroupIds(principal.getName()));

        if (searchQuery != null && !searchQuery.isBlank()) {
            model.addAttribute("searchQuery", searchQuery.trim());
            model.addAttribute("searchResults", userService.searchUsers(searchQuery, principal.getName()));
        }
    }
}
