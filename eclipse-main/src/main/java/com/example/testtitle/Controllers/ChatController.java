package com.example.testtitle.Controllers;

import com.example.testtitle.Models.ChatGroup;
import com.example.testtitle.Models.GroupMessage;
import com.example.testtitle.Models.User;
import com.example.testtitle.Services.GroupService;
import com.example.testtitle.Services.MessageService;
import com.example.testtitle.Services.PresenceService;
import com.example.testtitle.Services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final MessageService messageService;
    private final UserService userService;
    private final GroupService groupService;
    private final PresenceService presenceService;

    public ChatController(MessageService messageService,
                          UserService userService,
                          GroupService groupService,
                          PresenceService presenceService) {
        this.messageService = messageService;
        this.userService = userService;
        this.groupService = groupService;
        this.presenceService = presenceService;
    }

    @GetMapping("/account")
    public String accountRedirect() {
        return "redirect:/chat";
    }

    @GetMapping
    public String chatHome(Principal principal,
                         Model model,
                         @RequestParam(required = false) String q) {
        populateSidebar(model, principal, null, null, q);
        return "chat";
    }

    @GetMapping("/group/{groupId}")
    public String openGroupChat(@PathVariable Long groupId,
                                Principal principal,
                                Model model,
                                @RequestParam(required = false) String q) {
        ChatGroup group = groupService.getGroupForMember(groupId, principal.getName());
        groupService.markGroupAsRead(principal.getName(), groupId);

        model.addAttribute("group", group);
        model.addAttribute("groupMessages", groupService.getGroupHistory(groupId, principal.getName()));
        populateSidebar(model, principal, null, groupId, q);
        return "group-chat";
    }

    @GetMapping("/{receiverUsername}")
    public String openChat(@PathVariable String receiverUsername,
                           Principal principal,
                           Model model,
                           @RequestParam(required = false) String q) {
        if ("account".equals(receiverUsername) || "group".equals(receiverUsername)) {
            return "redirect:/chat";
        }

        if (receiverUsername.equals(principal.getName())) {
            return "redirect:/chat";
        }

        User receiver = userService.findByUsername(receiverUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь с username:" + receiverUsername + " не найден"));

        messageService.markConversationAsRead(principal.getName(), receiverUsername);

        model.addAttribute("messages", messageService.getChatHistory(principal.getName(), receiverUsername));
        model.addAttribute("receiver", receiver);
        model.addAttribute("receiverOnline", presenceService.isOnline(receiverUsername));
        populateSidebar(model, principal, receiverUsername, null, q);
        return "chat";
    }

    private void populateSidebar(Model model, Principal principal, String activeUsername, Long activeGroupId, String searchQuery) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUsername", principal.getName());
        model.addAttribute("activeUsername", activeUsername);
        model.addAttribute("activeGroupId", activeGroupId);
        model.addAttribute("conversations", messageService.getConversations(principal.getName()));
        model.addAttribute("groups", groupService.getGroupPreviews(principal.getName()));
        model.addAttribute("onlineUsers", presenceService.getOnlinePartners(principal.getName()));
        model.addAttribute("totalUnread", messageService.getTotalUnreadCount(principal.getName()));

        if (searchQuery != null && !searchQuery.isBlank()) {
            model.addAttribute("searchQuery", searchQuery.trim());
            model.addAttribute("searchResults", userService.searchUsers(searchQuery, principal.getName()));
        }
    }
}
