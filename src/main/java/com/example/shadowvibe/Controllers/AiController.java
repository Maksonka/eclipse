package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Services.AiService;
import com.example.shadowvibe.Services.PremiumService;
import com.example.shadowvibe.Services.TranslationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final PremiumService premiumService;
    private final TranslationService translationService;

    public AiController(AiService aiService, PremiumService premiumService, TranslationService translationService) {
        this.aiService = aiService;
        this.premiumService = premiumService;
        this.translationService = translationService;
    }

    @PostMapping("/translate")
    public ResponseEntity<?> translate(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestBody Map<String, Object> body) {
        if (!premiumService.isPremium(userDetails.getUsername())) {
            return premiumError();
        }
        String text = body.get("text") == null ? "" : String.valueOf(body.get("text"));
        String target = body.get("to") == null ? "" : String.valueOf(body.get("to"));
        if (text.isBlank()) {
            return ResponseEntity.ok(Map.of("translated", "", "from", "auto", "to", target));
        }
        String translated = aiService.translateText(text, target);
        return ResponseEntity.ok(Map.of("translated", translated, "from", "auto", "to", target));
    }

    @PostMapping("/digest")
    public ResponseEntity<?> digest(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestBody(required = false) Map<String, Object> body) {
        if (!premiumService.isPremium(userDetails.getUsername())) {
            return premiumError();
        }
        long since = 0L;
        if (body != null && body.get("since") instanceof Number n) {
            since = n.longValue();
        }
        return ResponseEntity.ok(aiService.digest(userDetails.getUsername(), since));
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestBody Map<String, Object> body) {
        if (!premiumService.isPremium(userDetails.getUsername())) {
            return premiumError();
        }
        String chatType = body.get("chatType") == null ? "direct" : String.valueOf(body.get("chatType"));
        String chatWith = body.get("chatWith") == null ? "" : String.valueOf(body.get("chatWith"));
        String query = body.get("query") == null ? "" : String.valueOf(body.get("query"));
        if (query.isBlank()) {
            return ResponseEntity.ok(Map.of("reply", "Напишите запрос ассистенту."));
        }
        String reply = aiService.assistantReply(userDetails.getUsername(), chatType, chatWith, query);
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    private ResponseEntity<Map<String, String>> premiumError() {
        return ResponseEntity.status(403)
                .body(Map.of("error", "Доступно только с Premium. Оформите подписку в /premium"));
    }
}
