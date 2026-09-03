package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Services.GhostModeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/settings/ghost")
public class GhostModeController {

    private final GhostModeService ghostModeService;

    public GhostModeController(GhostModeService ghostModeService) {
        this.ghostModeService = ghostModeService;
    }

    @PostMapping("/toggle")
    public String toggle(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            boolean enabled = ghostModeService.toggleGhostMode(principal.getName());
            redirectAttributes.addFlashAttribute("ghostMessage",
                    enabled ? "Режим Призрака включён" : "Режим Призрака выключен");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("ghostError", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/exceptions")
    public String addException(Principal principal,
                               @RequestParam String exceptionUsername,
                               @RequestParam(defaultValue = "false") boolean showActivity,
                               RedirectAttributes redirectAttributes) {
        try {
            ghostModeService.addException(principal.getName(), exceptionUsername, showActivity);
            redirectAttributes.addFlashAttribute("ghostMessage", "Исключение добавлено");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("ghostError", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/exceptions/delete")
    public String removeException(Principal principal,
                                  @RequestParam String exceptionUsername,
                                  RedirectAttributes redirectAttributes) {
        try {
            ghostModeService.removeException(principal.getName(), exceptionUsername);
            redirectAttributes.addFlashAttribute("ghostMessage", "Исключение удалено");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("ghostError", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/exceptions/update")
    public String updateException(Principal principal,
                                  @RequestParam String exceptionUsername,
                                  @RequestParam boolean showActivity,
                                  RedirectAttributes redirectAttributes) {
        try {
            ghostModeService.addException(principal.getName(), exceptionUsername, showActivity);
            redirectAttributes.addFlashAttribute("ghostMessage", "Исключение обновлено");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("ghostError", e.getMessage());
        }
        return "redirect:/settings";
    }
}
