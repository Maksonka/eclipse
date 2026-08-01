package com.example.testtitle.Controllers;

import com.example.testtitle.Models.User;
import com.example.testtitle.Services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/edit")
    public String editProfile(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        model.addAttribute("profileUser", user);
        return "profile-edit";
    }

    @PostMapping("/edit")
    public String saveProfile(Principal principal,
                              @RequestParam(required = false) String about,
                              @RequestParam(required = false) MultipartFile avatar,
                              RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfile(principal.getName(), about, avatar);
            redirectAttributes.addFlashAttribute("profileSaved", true);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("profileError", e.getMessage());
            return "redirect:/profile/edit";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("profileError", "Не удалось загрузить аватар");
            return "redirect:/profile/edit";
        }
        return "redirect:/profile/" + principal.getName();
    }

    @GetMapping("/{username}")
    public String viewProfile(@PathVariable String username,
                              Principal principal,
                              Model model) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        model.addAttribute("profileUser", user);
        model.addAttribute("isOwnProfile", principal.getName().equals(username));
        return "profile";
    }
}
