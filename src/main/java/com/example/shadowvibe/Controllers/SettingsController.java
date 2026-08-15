package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Services.GroupService;
import com.example.shadowvibe.Services.UserService;
import com.example.shadowvibe.enums.ThemePreference;
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
public class
SettingsController {

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
}
