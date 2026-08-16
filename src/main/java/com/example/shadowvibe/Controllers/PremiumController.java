package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Services.PremiumService;
import com.example.shadowvibe.Services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/premium")
public class PremiumController {

    private final PremiumService premiumService;
    private final UserService userService;

    public PremiumController(PremiumService premiumService, UserService userService) {
        this.premiumService = premiumService;
        this.userService = userService;
    }

    @GetMapping
    public String premiumPage(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        model.addAttribute("currentUser", user);
        model.addAttribute("premiumActive", user.isPremium());
        model.addAttribute("premiumUntilFormatted", premiumService.formatUntil(user.getPremiumUntil()));
        model.addAttribute("premiumTrialUsed", premiumService.isTrialUsed(principal.getName()));
        model.addAttribute("freeFavoritesLimit", PremiumService.FREE_FAVORITES_LIMIT);
        model.addAttribute("freeStickerPacks", PremiumService.FREE_STICKER_PACKS);
        model.addAttribute("freeGroupMembers", PremiumService.FREE_GROUP_MEMBERS);
        return "premium";
    }

    @PostMapping("/trial")
    public String activateTrial(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("premiumMessage", premiumService.activateTrial(principal.getName()));
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("premiumError", e.getMessage());
        }
        return "redirect:/premium";
    }

    @PostMapping("/activate-mock")
    public String activateMock(Principal principal, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("premiumMessage", premiumService.activateMock(principal.getName()));
        return "redirect:/premium";
    }

    @PostMapping("/cancel")
    public String cancel(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("premiumMessage", premiumService.cancel(principal.getName()));
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("premiumError", e.getMessage());
        }
        return "redirect:/premium";
    }
}
