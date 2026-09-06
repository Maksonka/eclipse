package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.UserRepository;
import com.example.shadowvibe.enums.ThemePreference;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    public static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public UserService(UserRepository userRepository,
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByUsernameOrEmail(String login) {
        return userRepository.findByUsernameOrEmail(login);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User registerUser(User user) {
        return userRepository.save(user);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public List<User> searchUsers(String query, String currentUsername) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepository.searchByUsername(query.trim(), currentUsername, PageRequest.of(0, 20));
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByUsernameAsc();
    }

    public User updateProfile(String username, String about, MultipartFile avatarFile) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setAbout(about != null ? about.trim() : null);

        if (avatarFile != null && !avatarFile.isEmpty()) {
            String contentType = avatarFile.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("Допустимы только изображения JPG, PNG, WEBP или GIF");
            }

            String extension = switch (contentType) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                default -> ".jpg";
            };

            deleteAvatarFile(user.getAvatarFilename());

            String filename = user.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path avatarDir = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
            Files.createDirectories(avatarDir);

            Path target = avatarDir.resolve(filename);
            Files.copy(avatarFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            user.setAvatarFilename(filename);
        }

        return userRepository.save(user);
    }

    public User updateTheme(String username, ThemePreference theme) {        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        // PREMIUM отключён: все темы доступны
        user.setThemePreference(theme);
        return userRepository.save(user);
    }

    @Transactional
    public void updatePrivacy(String username,
                              boolean hideOnlineStatus,
                              boolean searchable,
                              com.example.shadowvibe.enums.MessagesFrom messagesFrom) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.setHideOnlineStatus(hideOnlineStatus);
        user.setSearchable(searchable);
        user.setMessagesFrom(messagesFrom);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new WrongPasswordException("Текущий пароль указан неверно");
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Новый пароль должен содержать минимум " + MIN_PASSWORD_LENGTH + " символов");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void deleteAvatarFile(String filename) throws IOException {
        if (filename == null || filename.isBlank()) {
            return;
        }
        Path file = Paths.get(uploadDir, "avatars", filename).toAbsolutePath().normalize();
        Files.deleteIfExists(file);
    }
}