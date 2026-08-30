package com.example.shadowvibe.Services;

import com.example.shadowvibe.DTO.StickerDto;
import com.example.shadowvibe.DTO.StickerPackDto;
import com.example.shadowvibe.Models.Sticker;
import com.example.shadowvibe.Models.StickerPack;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Models.UserStickerPack;
import com.example.shadowvibe.Repositories.StickerPackRepository;
import com.example.shadowvibe.Repositories.StickerRepository;
import com.example.shadowvibe.Repositories.UserRepository;
import com.example.shadowvibe.Repositories.UserStickerPackRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class StickerService {

    public static final long MAX_STICKER_SIZE = 3L * 1024 * 1024;
    private static final int MAX_STICKERS_PER_REQUEST = 30;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final StickerRepository stickerRepository;
    private final StickerPackRepository stickerPackRepository;
    private final UserRepository userRepository;
    private final UserStickerPackRepository userStickerPackRepository;
    private final PremiumService premiumService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public StickerService(StickerRepository stickerRepository,
                          StickerPackRepository stickerPackRepository,
                          UserRepository userRepository,
                          UserStickerPackRepository userStickerPackRepository,
                          PremiumService premiumService) {
        this.stickerRepository = stickerRepository;
        this.stickerPackRepository = stickerPackRepository;
        this.userRepository = userRepository;
        this.userStickerPackRepository = userStickerPackRepository;
        this.premiumService = premiumService;
    }

    public List<StickerPackDto> listPacks(String currentUsername) {
        long userId = resolveUserId(currentUsername);
        Set<Long> subscribed = userId > 0
                ? userStickerPackRepository.findPackIdsByUserId(userId)
                : new HashSet<>();
        List<StickerPackDto> result = new ArrayList<>();
        for (StickerPack pack : stickerPackRepository.findAllByOrderByCreatedAtAsc()) {
            String author = pack.getAuthorUsername();
            boolean system = author == null || author.isBlank();
            boolean mine = author != null && author.equals(currentUsername);
            if (system || mine || subscribed.contains(pack.getId())) {
                result.add(toPackDto(pack, currentUsername, subscribed));
            }
        }
        return result;
    }

    public StickerPackDto getPackByStickerCode(String code, String currentUsername) {
        Sticker sticker = stickerRepository.findByCode(code).orElse(null);
        if (sticker == null || sticker.getPack() == null) {
            return null;
        }
        return toPackDto(sticker.getPack(), currentUsername);
    }

    public StickerPackDto subscribePack(String currentUsername, Long packId) {
        StickerPack pack = stickerPackRepository.findById(packId)
                .orElseThrow(() -> new IllegalArgumentException("Набор стикеров не найден"));

        String author = pack.getAuthorUsername();
        boolean system = author == null || author.isBlank();
        if (system || (author != null && author.equals(currentUsername))) {
            return toPackDto(pack, currentUsername);
        }

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (!userStickerPackRepository.existsByUserIdAndPackId(user.getId(), packId)) {
            userStickerPackRepository.save(new UserStickerPack(user, pack));
        }
        return toPackDto(pack, currentUsername);
    }

    private long resolveUserId(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        return userRepository.findByUsername(username).map(User::getId).orElse(0L);
    }

    public StickerPackDto createPack(String authorUsername, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Укажите название набора");
        }
        String clean = name.trim();
        if (clean.length() > 60) {
            throw new IllegalArgumentException("Название слишком длинное");
        }
        if (stickerPackRepository.findByNameIgnoreCase(clean).isPresent()) {
            throw new IllegalArgumentException("Набор с таким названием уже существует");
        }

        StickerPack pack = stickerPackRepository.save(new StickerPack(clean, authorUsername));
        return toPackDto(pack, authorUsername);
    }

    public StickerPackDto addStickers(String authorUsername, Long packId, MultipartFile[] files) throws IOException {
        StickerPack pack = stickerPackRepository.findById(packId)
                .orElseThrow(() -> new IllegalArgumentException("Набор стикеров не найден"));

        String owner = pack.getAuthorUsername();
        if (owner == null || owner.isBlank() || !owner.equals(authorUsername)) {
            throw new IllegalArgumentException("Добавлять стикеры может только создатель набора");
        }

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Выберите хотя бы один стикер");
        }
        if (files.length > MAX_STICKERS_PER_REQUEST) {
            throw new IllegalArgumentException("За один раз можно добавить не более " + MAX_STICKERS_PER_REQUEST + " стикеров");
        }

        long existing = stickerRepository.countByPackId(pack.getId());
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String url = saveStickerImage(file);
            existing++;
            Sticker sticker = new Sticker("pack" + pack.getId() + "_" + existing, url);
            pack.addSticker(sticker);
            stickerRepository.save(sticker);
        }
        return toPackDto(pack, authorUsername);
    }

    public Sticker findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return stickerRepository.findByCode(code).orElse(null);
    }

    public StickerDto toDto(Sticker sticker) {
        if (sticker == null) {
            return null;
        }
        return new StickerDto(sticker.getId(), sticker.getCode(), sticker.getFilename());
    }

    public StickerPackDto toPackDto(StickerPack pack, String currentUsername) {
        long userId = resolveUserId(currentUsername);
        Set<Long> subscribed = userId > 0
                ? userStickerPackRepository.findPackIdsByUserId(userId)
                : new HashSet<>();
        return toPackDto(pack, currentUsername, subscribed);
    }

    public StickerPackDto toPackDto(StickerPack pack, String currentUsername, Set<Long> subscribedPackIds) {
        List<StickerDto> stickers = new ArrayList<>();
        for (Sticker sticker : pack.getStickers()) {
            stickers.add(toDto(sticker));
        }
        String author = pack.getAuthorUsername();
        boolean system = author == null || author.isBlank();
        boolean mine = author != null && author.equals(currentUsername);
        boolean added = system || mine || subscribedPackIds.contains(pack.getId());
        return new StickerPackDto(
                pack.getId(),
                pack.getName(),
                author,
                system,
                mine,
                added,
                stickers
        );
    }

    private String saveStickerImage(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_STICKER_SIZE) {
            throw new IllegalArgumentException("Стикер слишком большой. Максимум 3 МБ");
        }
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Допустимы только изображения JPG, PNG, WEBP или GIF");
        }

        String extension = EXTENSIONS.get(contentType);
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;

        Path dir = Paths.get(uploadDir, "stickers").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/stickers/" + filename;
    }
}
