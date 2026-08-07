package com.example.testtitle.Services;

import com.example.testtitle.Models.User;
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

    public SidebarModelService(UserService userService,
                               MessageService messageService,
                               GroupService groupService,
                               PresenceService presenceService) {
        this.userService = userService;
        this.messageService = messageService;
        this.groupService = groupService;
        this.presenceService = presenceService;
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

        if (searchQuery != null && !searchQuery.isBlank()) {
            model.addAttribute("searchQuery", searchQuery.trim());
            model.addAttribute("searchResults", userService.searchUsers(searchQuery, principal.getName()));
        }
    }
}
