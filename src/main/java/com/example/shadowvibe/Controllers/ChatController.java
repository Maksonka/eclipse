package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.ChatGroup;
import com.example.shadowvibe.Models.GroupMessage;
import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Services.GroupService;
import com.example.shadowvibe.Services.MessageService;
import com.example.shadowvibe.Services.PresenceService;
import com.example.shadowvibe.Services.SidebarModelService;
import com.example.shadowvibe.Services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final MessageService messageService;
    private final UserService userService;
    private final GroupService groupService;
    private final PresenceService presenceService;
    private final SidebarModelService sidebarModelService;

    public ChatController(MessageService messageService,
                          UserService userService,
                          GroupService groupService,
                          PresenceService presenceService,
                          SidebarModelService sidebarModelService) {
        this.messageService = messageService;
        this.userService = userService;
        this.groupService = groupService;
        this.presenceService = presenceService;
        this.sidebarModelService = sidebarModelService;
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

    @PostMapping("/{receiverUsername}/delete")
    public String deleteConversation(@PathVariable String receiverUsername,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        if (!"account".equals(receiverUsername) && !"group".equals(receiverUsername)
                && !receiverUsername.equals(principal.getName())) {
            messageService.deleteConversation(principal.getName(), receiverUsername);
            redirectAttributes.addFlashAttribute("conversationDeleted", "Переписка удалена");
        }
        return "redirect:/chat";
    }

    @PostMapping("/{receiverUsername}/attachment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendAttachment(@PathVariable String receiverUsername,
                                                              Principal principal,
                                                              @RequestParam("file") MultipartFile file) {
        if (receiverUsername.equals(principal.getName())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Нельзя отправить файл самому себе"));
        }
        try {
            Message saved = messageService.saveAttachmentMessage(principal.getName(), receiverUsername, file);
            messageService.broadcastDirectMessage(saved);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Не удалось сохранить файл"));
        }
    }

    private void populateSidebar(Model model, Principal principal, String activeUsername, Long activeGroupId, String searchQuery) {
        sidebarModelService.populate(model, principal, activeUsername, activeGroupId, searchQuery);
    }
}
