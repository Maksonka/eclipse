package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Services.PaymentService;
import com.example.shadowvibe.Services.PremiumService;
import com.example.shadowvibe.Services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/premium")
public class PremiumController {

    private static final String SESSION_PAYMENT_ID = "premiumPaymentId";

    private final PremiumService premiumService;
    private final UserService userService;
    private final PaymentService paymentService;

    public PremiumController(PremiumService premiumService, UserService userService, PaymentService paymentService) {
        this.premiumService = premiumService;
        this.userService = userService;
        this.paymentService = paymentService;
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
        model.addAttribute("premiumPrice", paymentService.getAmountRub());
        model.addAttribute("premiumDays", paymentService.getDays());
        model.addAttribute("paymentConfigured", paymentService.isConfigured());
        return "premium";
    }

    @PostMapping("/checkout")
    public RedirectView checkout(Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        PaymentService.CreatedPayment payment;
        try {
            payment = paymentService.createPayment(principal.getName());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("premiumError", e.getMessage());
            return new RedirectView("/premium", true);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("premiumError", "Не удалось создать платёж: " + e.getMessage());
            return new RedirectView("/premium", true);
        }
        if (payment.id == null || payment.confirmationUrl == null) {
            redirectAttributes.addFlashAttribute("premiumError", "Платёж не создан (нет confirmation_url)");
            return new RedirectView("/premium", true);
        }
        session.setAttribute(SESSION_PAYMENT_ID, payment.id);
        return new RedirectView(payment.confirmationUrl);
    }

    @GetMapping(value = "/api/payment-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> paymentStatus(Principal principal, HttpSession session) {
        Map<String, String> result = new LinkedHashMap<>();
        Object stored = session.getAttribute(SESSION_PAYMENT_ID);
        if (stored == null) {
            result.put("state", "none");
            return result;
        }
        String paymentId = stored.toString();
        String state;
        try {
            state = paymentService.getStatus(paymentId);
        } catch (Exception e) {
            state = "unknown";
        }
        if ("succeeded".equals(state)) {
            try {
                paymentService.activateIfPaid(paymentId, principal.getName());
            } catch (Exception e) {
                state = "unknown";
            }
            session.removeAttribute(SESSION_PAYMENT_ID);
        } else if ("canceled".equals(state)) {
            session.removeAttribute(SESSION_PAYMENT_ID);
        }
        result.put("state", state == null ? "none" : state);
        return result;
    }

    @PostMapping(value = "/notification", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String handleNotification(@RequestBody String jsonBody,
                                     @RequestHeader(value = "Content-Signature", required = false) String signature) {
        if (signature == null || signature.isBlank()) {
            return "missing-signature";
        }
        if (!paymentService.verifySignature(jsonBody, signature)) {
            return "invalid-signature";
        }
        paymentService.handleNotification(jsonBody);
        return "OK";
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