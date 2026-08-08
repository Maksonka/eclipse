package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.ChatGroup;
import com.example.shadowvibe.Models.GroupMessage;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Services.GroupService;
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
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/group")
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;

    public GroupController(GroupService groupService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;
    }

    @GetMapping("/create")
    public String createGroupForm(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        model.addAttribute("currentUser", user);
        model.addAttribute("groups", groupService.getGroupPreviews(principal.getName()));
        return "group-create";
    }

    @PostMapping("/create")
    public String createGroup(Principal principal,
                              @RequestParam String name,
                              @RequestParam(required = false) String members,
                              RedirectAttributes redirectAttributes) {
        try {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Укажите название группы");
            }
            groupService.createGroup(principal.getName(), name, members);
            redirectAttributes.addFlashAttribute("groupCreated", true);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("groupError", e.getMessage());
        }
        return "redirect:/group/create";
    }

    @GetMapping("/{groupId}/settings")
    public String groupSettings(@PathVariable Long groupId,
                                Principal principal,
                                Model model) {
        ChatGroup group = groupService.getGroupForMember(groupId, principal.getName());
        List<User> members = groupService.getGroupMembers(groupId, principal.getName());
        boolean isCreator = group.getCreatedBy().getUsername().equals(principal.getName());

        model.addAttribute("currentUser", userService.findByUsername(principal.getName()).orElse(null));
        model.addAttribute("group", group);
        model.addAttribute("groupMembers", members);
        model.addAttribute("isCreator", isCreator);
        return "group-settings";
    }

    @PostMapping("/{groupId}/avatar")
    public String uploadGroupAvatar(@PathVariable Long groupId,
                                    Principal principal,
                                    @RequestParam("avatar") MultipartFile avatar,
                                    RedirectAttributes redirectAttributes) {
        try {
            groupService.updateGroupAvatar(groupId, principal.getName(), avatar);
            redirectAttributes.addFlashAttribute("avatarSuccess", "Аватар группы обновлён");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("avatarError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("avatarError", "Не удалось загрузить аватар: " + e.getMessage());
        }
        return "redirect:/group/" + groupId + "/settings";
    }

    @PostMapping("/{groupId}/attachment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendAttachment(@PathVariable Long groupId,
                                                              Principal principal,
                                                              @RequestParam("file") MultipartFile file) {
        try {
            GroupMessage saved = groupService.saveGroupAttachmentMessage(principal.getName(), groupId, file);
            groupService.broadcastGroupMessage(saved);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Не удалось сохранить файл"));
        }
    }

    @PostMapping("/{groupId}/members/{username}/remove")
    public String removeMember(@PathVariable Long groupId,
                               @PathVariable String username,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            groupService.removeMember(groupId, principal.getName(), username);
            redirectAttributes.addFlashAttribute("groupSuccess", "Участник " + username + " удалён из группы");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("avatarError", e.getMessage());
        }
        return "redirect:/group/" + groupId + "/settings";
    }

    @PostMapping("/{groupId}/members/add")
    public String addMembers(@PathVariable Long groupId,
                             @RequestParam("members") String members,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            groupService.addMembers(groupId, principal.getName(), members);
            redirectAttributes.addFlashAttribute("groupSuccess", "Участники добавлены в группу");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("groupError", e.getMessage());
        }
        return "redirect:/group/" + groupId + "/settings";
    }

    @PostMapping("/{groupId}/delete")
    public String deleteGroup(@PathVariable Long groupId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            groupService.deleteGroup(groupId, principal.getName());
            redirectAttributes.addFlashAttribute("groupDeleted", "Группа удалена");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("avatarError", e.getMessage());
            return "redirect:/group/" + groupId + "/settings";
        }
        return "redirect:/chat";
    }
}
