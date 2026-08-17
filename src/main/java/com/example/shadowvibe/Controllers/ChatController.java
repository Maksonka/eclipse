package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.ChatGroup;
import com.example.shadowvibe.Models.GroupMessage;
import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Services.AttachmentService;
import com.example.shadowvibe.Services.E2eKeyService;
import com.example.shadowvibe.Services.GroupService;
import com.example.shadowvibe.Services.MessageService;
import com.example.shadowvibe.Services.MuteService;
import com.example.shadowvibe.Services.PresenceService;
import com.example.shadowvibe.Services.ReactionService;
import com.example.shadowvibe.Services.SidebarModelService;
import com.example.shadowvibe.Services.UserService;
import com.example.shadowvibe.enums.ReactionTargetType;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final MessageService messageService;
    private final UserService userService;
    private final GroupService groupService;
    private final PresenceService presenceService;
    private final SidebarModelService sidebarModelService;
    private final ReactionService reactionService;
    private final MuteService muteService;
    private final E2eKeyService e2eKeyService;

    public ChatController(MessageService messageService,
                          UserService userService,
                          GroupService groupService,
                          PresenceService presenceService,
                          SidebarModelService sidebarModelService,
                          ReactionService reactionService,
                          MuteService muteService,
                          E2eKeyService e2eKeyService) {
        this.messageService = messageService;
        this.userService = userService;
        this.groupService = groupService;
        this.presenceService = presenceService;
        this.sidebarModelService = sidebarModelService;
        this.reactionService = reactionService;
        this.muteService = muteService;
        this.e2eKeyService = e2eKeyService;
    }

    @GetMapping("/account")
    public String accountRedirect() {
        return "redirect:/chat";
    }

    @GetMapping
    public String chatHome(Principal principal,
                         Model model,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) String mode) {
        populateSidebar(model, principal, null, null, q, mode);
        return "chat";
    }

    @GetMapping("/group/{groupId}")
    public String openGroupChat(@PathVariable Long groupId,
                                Principal principal,
                                Model model,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) String mode) {
        ChatGroup group = groupService.getGroupForMember(groupId, principal.getName());
        groupService.markGroupAsRead(principal.getName(), groupId);

        List<GroupMessage> groupMessages = groupService.getGroupHistory(groupId, principal.getName());
        model.addAttribute("group", group);
        model.addAttribute("groupMuted", muteService.isGroupMuted(principal.getName(), groupId));
        model.addAttribute("groupMessages", groupMessages);
        model.addAttribute("reactionsByMessage",
                reactionService.getReactionsBatch(ReactionTargetType.GROUP,
                        groupMessages.stream().map(GroupMessage::getId).toList()));
        addPinnedGroupMessage(model, groupId, principal.getName());
        populateSidebar(model, principal, null, groupId, q, mode);
        return "group-chat";
    }

    @GetMapping("/{receiverUsername}")
    public String openChat(@PathVariable String receiverUsername,
                           Principal principal,
                           Model model,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String mode) {
        if ("account".equals(receiverUsername) || "group".equals(receiverUsername)) {
            return "redirect:/chat";
        }

        if (receiverUsername.equals(principal.getName())) {
            return "redirect:/chat";
        }

        User receiver = userService.findByUsername(receiverUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь с username:" + receiverUsername + " не найден"));

        messageService.markConversationAsRead(principal.getName(), receiverUsername);

        List<Message> messages = messageService.getChatHistory(principal.getName(), receiverUsername);
        model.addAttribute("messages", messages);
        model.addAttribute("reactionsByMessage",
                reactionService.getReactionsBatch(ReactionTargetType.DIRECT,
                        messages.stream().map(Message::getId).toList()));
        addPinnedDirectMessage(model, principal.getName(), receiverUsername);
        model.addAttribute("receiver", receiver);
        model.addAttribute("chatMuted", muteService.isDirectMuted(principal.getName(), receiverUsername));
        model.addAttribute("receiverOnline", presenceService.isOnline(receiverUsername));
        model.addAttribute("peerE2e", e2eKeyService.hasKey(receiverUsername));
        populateSidebar(model, principal, receiverUsername, null, q, mode);
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
                                                              @RequestParam("file") MultipartFile file,
                                                              @RequestParam(value = "content", required = false) String content,
                                                              @RequestParam(value = "replyToMessageId", required = false) Long replyToMessageId) {
        if (receiverUsername.equals(principal.getName())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Нельзя отправить файл самому себе"));
        }
        try {
            Message saved = messageService.saveAttachmentMessage(principal.getName(), receiverUsername, file, content, replyToMessageId);
            messageService.broadcastDirectMessage(saved);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Не удалось сохранить файл"));
        }
    }

    private void addPinnedDirectMessage(Model model, String username, String partnerUsername) {
        List<Message> pinned = messageService.getPinnedDirectMessages(username, partnerUsername).stream()
                .filter(m -> !isDeletedForMe(m, username))
                .collect(Collectors.toList());
        model.addAttribute("pinnedMessages", pinned);
        model.addAttribute("pinnedPreviews", pinned.stream()
                .collect(Collectors.toMap(Message::getId, this::pinnedDirectPreview)));
    }

    private boolean isDeletedForMe(Message message, String username) {
        if (message.getSender().getUsername().equals(username)) {
            return message.isDeletedBySender();
        }
        return message.isDeletedByReceiver();
    }

    private String pinnedDirectPreview(Message message) {
        String preview;
        if (message.hasSticker()) {
            preview = "Стикер";
        } else if (message.hasAudio()) {
            preview = "Голосовое сообщение";
        } else if (message.getContent() != null && !message.getContent().isBlank()) {
            preview = message.getContent();
        } else {
            preview = AttachmentService.labelForType(message.getAttachmentType());
        }
        return preview.length() > 60 ? preview.substring(0, 60) + "…" : preview;
    }

    private void addPinnedGroupMessage(Model model, Long groupId, String username) {
        List<GroupMessage> pinned = groupService.getPinnedGroupMessages(groupId, username);
        User currentUser = userService.findByUsername(username).orElse(null);
        if (currentUser != null) {
            pinned = pinned.stream()
                    .filter(m -> !m.getDeletedByUserIds().contains(currentUser.getId()))
                    .collect(Collectors.toList());
        }
        model.addAttribute("pinnedMessages", pinned);
        model.addAttribute("pinnedPreviews", pinned.stream()
                .collect(Collectors.toMap(GroupMessage::getId, this::pinnedGroupPreview)));
    }

    private String pinnedGroupPreview(GroupMessage message) {
        String preview;
        if (message.hasSticker()) {
            preview = "Стикер";
        } else if (message.getContent() != null && !message.getContent().isBlank()) {
            preview = message.getContent();
        } else {
            preview = AttachmentService.labelForType(message.getAttachmentType());
        }
        return preview.length() > 60 ? preview.substring(0, 60) + "…" : preview;
    }

    private void populateSidebar(Model model, Principal principal, String activeUsername, Long activeGroupId, String searchQuery, String mode) {
        sidebarModelService.populate(model, principal, activeUsername, activeGroupId, searchQuery, mode);
    }
}
