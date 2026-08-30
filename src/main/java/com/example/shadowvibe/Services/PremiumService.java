package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class PremiumService {

    public static final int FREE_FAVORITES_LIMIT = 50;
    public static final int FREE_STICKER_PACKS = 2;
    public static final int FREE_GROUP_MEMBERS = 20;
    public static final int FREE_GROUP_MEMBER_LIMIT = 5;
    public static final int PREMIUM_GROUP_MEMBER_LIMIT = 500;
    public static final long FREE_FILE_SIZE = 5L * 1024 * 1024;
    public static final long PREMIUM_FILE_SIZE = 50L * 1024 * 1024;
    public static final long FREE_VOICE_SIZE = 5L * 1024 * 1024;
    public static final long PREMIUM_VOICE_SIZE = 25L * 1024 * 1024;

    public static final int TRIAL_DAYS = 7;
    public static final int MOCK_DAYS = 30;

    private static final DateTimeFormatter UNTIL_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final UserRepository userRepository;

    public PremiumService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isPremium(String username) {
        return findByUsername(username).map(User::isPremium).orElse(false);
    }

    public Optional<LocalDateTime> premiumUntil(String username) {
        return findByUsername(username).map(User::getPremiumUntil);
    }

    public boolean isTrialUsed(String username) {
        return findByUsername(username).map(User::isPremiumTrialUsed).orElse(false);
    }

    public String activateTrial(String username) {
        User user = requireUser(username);
        if (user.isPremium()) {
            throw new IllegalStateException("У вас уже активна подписка Premium");
        }
        if (user.isPremiumTrialUsed()) {
            throw new IllegalStateException("Пробный период уже был использован");
        }
        user.setPremiumTrialUsed(true);
        user.setPremiumUntil(LocalDateTime.now().plusDays(TRIAL_DAYS));
        userRepository.save(user);
        return "Пробный период Premium активирован на " + TRIAL_DAYS + " дней";
    }

    public String activateMock(String username) {
        User user = requireUser(username);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = user.getPremiumUntil() != null && user.getPremiumUntil().isAfter(now)
                ? user.getPremiumUntil()
                : now;
        user.setPremiumUntil(base.plusDays(MOCK_DAYS));
        userRepository.save(user);
        return "Premium активирован на " + MOCK_DAYS + " дней (тестовая оплата)";
    }

    public String cancel(String username) {
        User user = requireUser(username);
        if (!user.isPremium()) {
            throw new IllegalStateException("Подписка не активна");
        }
        user.setPremiumUntil(LocalDateTime.now());
        userRepository.save(user);
        return "Подписка Premium отменена";
    }

    public void enforceFileSize(String username, long size) {
        long max = maxAttachmentBytes(username);
        if (size > max) {
            throw new IllegalArgumentException("Файл слишком большой. На бесплатном тарифе максимум "
                    + formatMb(FREE_FILE_SIZE) + ", файлы до " + formatMb(PREMIUM_FILE_SIZE) + " доступны с Premium");
        }
    }

    public void enforceVoiceSize(String username, long size) {
        long max = maxVoiceBytes(username);
        if (size > max) {
            throw new IllegalArgumentException("Голосовое слишком длинное. На бесплатном тарифе голосовые до "
                    + formatMb(FREE_VOICE_SIZE) + ", более длинные доступны с Premium");
        }
    }

    public long maxAttachmentBytes(String username) {
        return isPremium(username) ? PREMIUM_FILE_SIZE : FREE_FILE_SIZE;
    }

    public long maxVoiceBytes(String username) {
        return isPremium(username) ? PREMIUM_VOICE_SIZE : FREE_VOICE_SIZE;
    }

    public int maxRoomMembers(String hostUsername) {
        return isPremium(hostUsername) ? PREMIUM_GROUP_MEMBER_LIMIT : FREE_GROUP_MEMBER_LIMIT;
    }

    public void enforceRoomMemberLimit(String hostUsername, int currentMembers) {
        int max = maxRoomMembers(hostUsername);
        if (currentMembers >= max) {
            throw new IllegalArgumentException("Комната заполнена. На бесплатном тарифе до " + FREE_GROUP_MEMBER_LIMIT
                    + " участников, до " + PREMIUM_GROUP_MEMBER_LIMIT + " доступно с Premium");
        }
    }

    public String formatUntil(LocalDateTime until) {
        return until == null ? null : until.format(UNTIL_FORMATTER);
    }

    private String formatMb(long bytes) {
        return bytes / (1024 * 1024) + " МБ";
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    private Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }
}
