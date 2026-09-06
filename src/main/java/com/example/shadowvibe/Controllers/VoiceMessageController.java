package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Services.AttachmentService;
import com.example.shadowvibe.Services.WhisperService;
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
    private final WhisperService whisperService;

    public VoiceMessageController(AttachmentService attachmentService,
                                  WhisperService whisperService) {
        this.attachmentService = attachmentService;
        this.whisperService = whisperService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "durationMs", required = false) Long durationMs,
                                    Principal principal) {
        try {
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

    @PostMapping("/transcribe")
    public ResponseEntity<?> transcribe(@RequestParam("file") MultipartFile file, Principal principal) {
        // PREMIUM отключён: расшифровка доступна всем
        if (!whisperService.isAvailable()) {
            return ResponseEntity.status(503).body(Map.of("error", "Распознавание временно недоступно"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Пустой аудиофайл"));
        }
        long maxBytes = 5L * 1024 * 1024; // PremiumService.FREE_VOICE_SIZE (PREMIUM отключён)
        if (file.getSize() > maxBytes) {
            return ResponseEntity.badRequest().body(Map.of("error", "Файл слишком большой"));
        }
        try {
            String transcript = whisperService.transcribe(file.getBytes());
            return ResponseEntity.ok(Map.of("transcript", transcript));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Не удалось прочитать аудиофайл"));
        }
    }
}
