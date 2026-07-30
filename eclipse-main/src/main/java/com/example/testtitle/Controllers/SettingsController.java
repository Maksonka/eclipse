package com.example.testtitle.Controllers;

import com.example.testtitle.Models.User;
import com.example.testtitle.Services.GroupService;
import com.example.testtitle.Services.UserService;
import com.example.testtitle.enums.ThemePreference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final UserService userService;
    private final GroupService groupService;

    public SettingsController(UserService userService, GroupService groupService) {
        this.userService = userService;
        this.groupService = groupService;
    }

    @GetMapping
    public String settings(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        model.addAttribute("currentUser", user);
        model.addAttribute("groups", groupService.getGroupPreviews(principal.getName()));
        return "settings";
    }

    @PostMapping("/theme")
    public String updateTheme(Principal principal,
                              @RequestParam ThemePreference theme,
                              RedirectAttributes redirectAttributes) {
        userService.updateTheme(principal.getName(), theme);
        redirectAttributes.addFlashAttribute("settingsSaved", true);
        return "redirect:/settings";
    }

    @PostMapping("/group")
    public String createGroup(Principal principal,
                              @RequestParam String name,
                              @RequestParam(required = false) String members,
                              RedirectAttributes redirectAttributes) {
        try {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Укажите название группы");
            }
            groupService.createGroup(principal.getName(), name, members);
            redirectAttributes.addFlashAttribute("settingsSaved", true);
            redirectAttributes.addFlashAttribute("groupCreated", true);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("settingsError", e.getMessage());
        }
        return "redirect:/settings";
    }
}
