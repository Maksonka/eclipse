package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Services.AttachmentService;
import com.example.shadowvibe.Services.PremiumService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
public class VoiceMessageController {

    private final AttachmentService attachmentService;
    private final PremiumService premiumService;

    public VoiceMessageController(AttachmentService attachmentService, PremiumService premiumService) {
        this.attachmentService = attachmentService;
        this.premiumService = premiumService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "durationMs", required = false) Long durationMs,
                                    Principal principal) {
        try {
            premiumService.enforceVoiceSize(principal.getName(), file.getSize());
            AttachmentService.AttachmentInfo info = attachmentService.saveAudio(file);
            return ResponseEntity.ok(Map.of(
                    "url", "/uploads/voice/" + info.filename(),
                    "durationMs", durationMs != null ? durationMs : 0,
                    "size", info.size()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Не удалось сохранить файл"));
        }
    }
}
