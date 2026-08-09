package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.DTO.StickerPackDto;
import com.example.shadowvibe.Services.StickerService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class StickerController {

    private final StickerService stickerService;

    public StickerController(StickerService stickerService) {
        this.stickerService = stickerService;
    }

    @GetMapping("/stickers")
    @ResponseBody
    public List<StickerPackDto> listPacks() {
        return stickerService.listPacks();
    }

    @PostMapping("/sticker-packs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPack(Principal principal,
                                                          @RequestParam("name") String name) {
        try {
            StickerPackDto pack = stickerService.createPack(principal.getName(), name);
            return ResponseEntity.ok(Map.of("success", true, "pack", pack));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sticker-packs/{packId}/stickers")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addStickers(Principal principal,
                                                           @PathVariable Long packId,
                                                           @RequestParam("files") MultipartFile[] files) {
        try {
            StickerPackDto pack = stickerService.addStickers(principal.getName(), packId, files);
            return ResponseEntity.ok(Map.of("success", true, "pack", pack));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Не удалось сохранить стикер"));
        }
    }
}
