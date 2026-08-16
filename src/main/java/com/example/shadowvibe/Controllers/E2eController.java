package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Services.PremiumService;
import com.example.shadowvibe.Services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class E2eController {

    private final PremiumService premiumService;
    private final UserService userService;

    public E2eController(PremiumService premiumService, UserService userService) {
        this.premiumService = premiumService;
        this.userService = userService;
    }

    @GetMapping("/e2e")
    public String e2ePage(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        model.addAttribute("currentUser", user);
        model.addAttribute("premiumActive", user.isPremium());
        model.addAttribute("premiumUntilFormatted", premiumService.formatUntil(user.getPremiumUntil()));
        return "e2e";
    }
}
