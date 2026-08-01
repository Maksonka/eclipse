package com.example.testtitle.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AttachmentService {

    public static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo", "video/ogg"
    );

    private static final Set<String> AUDIO_TYPES = Set.of(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/ogg", "audio/aac",
            "audio/mp4", "audio/x-m4a", "audio/x-aac"
    );

    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/webp", ".webp"),
            Map.entry("image/gif", ".gif"),
            Map.entry("video/mp4", ".mp4"),
            Map.entry("video/webm", ".webm"),
            Map.entry("video/quicktime", ".mov"),
            Map.entry("video/x-msvideo", ".avi"),
            Map.entry("video/ogg", ".ogv"),
            Map.entry("audio/mpeg", ".mp3"),
            Map.entry("audio/mp3", ".mp3"),
            Map.entry("audio/wav", ".wav"),
            Map.entry("audio/x-wav", ".wav"),
            Map.entry("audio/ogg", ".ogg"),
            Map.entry("audio/aac", ".aac"),
            Map.entry("audio/x-aac", ".aac"),
            Map.entry("audio/mp4", ".m4a"),
            Map.entry("audio/x-m4a", ".m4a")
    );

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public record AttachmentInfo(String filename, String type, String originalName, long size) {
    }

    public AttachmentInfo save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Выберите файл для отправки");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Файл слишком большой. Максимальный размер — 50 МБ");
        }

        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        String type = categorize(contentType);
        String extension = extensionFor(contentType, file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;

        Path dir = Paths.get(uploadDir, "messages").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return new AttachmentInfo(filename, type, file.getOriginalFilename(), file.getSize());
    }

    public static String labelForType(String type) {
        if (type == null) {
            return "Файл";
        }
        return switch (type) {
            case "image" -> "Фото";
            case "video" -> "Видео";
            case "audio" -> "Аудио";
            default -> "Файл";
        };
    }

    public static String formatSize(Long bytes) {
        if (bytes == null) {
            return "";
        }
        if (bytes < 1024) {
            return bytes + " Б";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f КБ", bytes / 1024.0).replace('.', ',');
        }
        return String.format("%.1f МБ", bytes / (1024.0 * 1024.0)).replace('.', ',');
    }

    private String categorize(String contentType) {
        if (IMAGE_TYPES.contains(contentType)) {
            return "image";
        }
        if (VIDEO_TYPES.contains(contentType)) {
            return "video";
        }
        if (AUDIO_TYPES.contains(contentType)) {
            return "audio";
        }
        return "file";
    }

    private String extensionFor(String contentType, String originalFilename) {
        String ext = EXTENSIONS.get(contentType);
        if (ext != null) {
            return ext;
        }
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                String candidate = originalFilename.substring(dot).toLowerCase().replaceAll("[^a-z0-9.]", "");
                if (candidate.length() > 1 && candidate.length() <= 12) {
                    return candidate;
                }
            }
        }
        return ".file";
    }
}
