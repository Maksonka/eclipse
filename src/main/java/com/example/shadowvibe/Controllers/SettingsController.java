package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Services.AttemptRateLimiter;
import com.example.shadowvibe.Services.BlockService;
import com.example.shadowvibe.Services.GroupService;
import com.example.shadowvibe.Services.UserService;
import com.example.shadowvibe.Services.WrongPasswordException;
import com.example.shadowvibe.enums.MessagesFrom;
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
    private final BlockService blockService;
    private final AttemptRateLimiter rateLimiter;

    public SettingsController(UserService userService,
                              GroupService groupService,
                              BlockService blockService,
                              AttemptRateLimiter rateLimiter) {
        this.userService = userService;
        this.groupService = groupService;
        this.blockService = blockService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public String settings(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        List<String> blockedUsernames = blockService.getBlockedUsernames(principal.getName());
        model.addAttribute("currentUser", user);
        model.addAttribute("blockedCount", blockedUsernames.size());
        return "settings";
    }

    @GetMapping("/blocked")
    public String blockedPage(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        List<User> blockedUsers = blockService.getBlockedUsernames(principal.getName()).stream()
                .map(username -> userService.findByUsername(username).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        model.addAttribute("currentUser", user);
        model.addAttribute("blockedUsers", blockedUsers);
        return "settings-blocked";
    }

    @PostMapping("/theme")
    public String updateTheme(Principal principal,
                              @RequestParam ThemePreference theme,
                              RedirectAttributes redirectAttributes) {
        try {
            userService.updateTheme(principal.getName(), theme);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("settingsError", e.getMessage());
            return "redirect:/settings";
        }
        redirectAttributes.addFlashAttribute("settingsSaved", true);
        return "redirect:/settings";
    }

    @PostMapping("/privacy")
    public String updatePrivacy(Principal principal,
                                @RequestParam(defaultValue = "false") boolean hideOnlineStatus,
                                @RequestParam(defaultValue = "false") boolean searchable,
                                @RequestParam(defaultValue = "ALL") MessagesFrom messagesFrom,
                                RedirectAttributes redirectAttributes) {
        userService.updatePrivacy(principal.getName(), hideOnlineStatus, searchable, messagesFrom);
        redirectAttributes.addFlashAttribute("settingsSaved", true);
        return "redirect:/settings";
    }

    @PostMapping("/password")
    public String changePassword(Principal principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        String key = AttemptRateLimiter.KEY_PASSWORD_CHANGE + principal.getName();
        if (rateLimiter.isBlocked(key)) {
            redirectAttributes.addFlashAttribute("settingsError",
                    "Слишком много неудачных попыток. Повторите через "
                            + rateLimiter.remainingLockMinutes(key) + " мин");
            return "redirect:/settings";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("settingsError", "Пароли не совпадают");
            return "redirect:/settings";
        }
        try {
            userService.changePassword(principal.getName(), currentPassword, newPassword);
        } catch (WrongPasswordException e) {
            rateLimiter.recordFailure(key);
            redirectAttributes.addFlashAttribute("settingsError", e.getMessage());
            return "redirect:/settings";
        }
        rateLimiter.recordSuccess(key);
        redirectAttributes.addFlashAttribute("settingsSaved", true);
        redirectAttributes.addFlashAttribute("savedSection", "password");
        return "redirect:/settings";
    }

    @PostMapping("/unblock")
    public String unblockUser(Principal principal,
                              @RequestParam String username,
                              RedirectAttributes redirectAttributes) {
        blockService.unblock(principal.getName(), username);
        redirectAttributes.addFlashAttribute("settingsSaved", true);
        return "redirect:/settings/blocked";
    }
}
