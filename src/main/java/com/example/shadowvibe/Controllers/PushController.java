package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.DTO.PushSubscriptionRequest;
import com.example.shadowvibe.Services.PushService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushService pushService;

    public PushController(PushService pushService) {
        this.pushService = pushService;
    }

    @GetMapping("/public-key")
    public Map<String, String> publicKey() {
        return Map.of("publicKey", pushService.getPublicKey());
    }

    @PostMapping("/register")
    public Map<String, Boolean> register(@RequestBody PushSubscriptionRequest request, Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Не авторизован");
        }
        pushService.register(principal.getName(), request.getEndpoint(), request.getP256dh(), request.getAuth());
        return Map.of("ok", true);
    }

    @DeleteMapping("/unregister")
    public Map<String, Boolean> unregister(@RequestBody PushSubscriptionRequest request, Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Не авторизован");
        }
        pushService.unregister(principal.getName(), request.getEndpoint());
        return Map.of("ok", true);
    }
}
