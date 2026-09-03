package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.GhostException;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.GhostExceptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GhostModeService {

    private final GhostExceptionRepository ghostExceptionRepository;
    private final UserService userService;

    public GhostModeService(GhostExceptionRepository ghostExceptionRepository, UserService userService) {
        this.ghostExceptionRepository = ghostExceptionRepository;
        this.userService = userService;
    }

    public boolean isGhostModeEnabled(String username) {
        User user = userService.findByUsername(username).orElse(null);
        return user != null && user.isGhostMode();
    }

    public boolean shouldSuppressTyping(String senderUsername, String receiverUsername) {
        if (!isGhostModeEnabled(senderUsername)) {
            return false;
        }
        return !hasShowActivityException(senderUsername, receiverUsername);
    }

    public boolean shouldSuppressReadReceipt(String readerUsername, String partnerUsername) {
        if (!isGhostModeEnabled(readerUsername)) {
            return false;
        }
        return !hasShowActivityException(readerUsername, partnerUsername);
    }

    public boolean shouldSuppressGroupTyping(String senderUsername) {
        return isGhostModeEnabled(senderUsername);
    }

    private boolean hasShowActivityException(String ghostUsername, String otherUsername) {
        return ghostExceptionRepository.findByUsernameAndExceptionUsername(ghostUsername, otherUsername)
                .map(GhostException::isShowActivity)
                .orElse(false);
    }

    @Transactional
    public boolean toggleGhostMode(String username) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!user.isPremium()) {
            throw new RuntimeException("Режим Призрака доступен только с Premium");
        }
        user.setGhostMode(!user.isGhostMode());
        userService.save(user);
        return user.isGhostMode();
    }

    @Transactional
    public List<Map<String, Object>> getExceptions(String username) {
        return ghostExceptionRepository.findByUsername(username).stream()
                .map(ge -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", ge.getId());
                    map.put("username", ge.getExceptionUser().getUsername());
                    map.put("showActivity", ge.isShowActivity());
                    User exceptionUser = ge.getExceptionUser();
                    map.put("avatarFilename", exceptionUser.getAvatarFilename());
                    return map;
                })
                .toList();
    }

    @Transactional
    public void addException(String username, String exceptionUsername, boolean showActivity) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        User exceptionUser = userService.findByUsername(exceptionUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (user.getId().equals(exceptionUser.getId())) {
            throw new RuntimeException("Нельзя добавить себя в исключения");
        }
        ghostExceptionRepository.findByUsernameAndExceptionUsername(username, exceptionUsername)
                .ifPresentOrElse(
                        existing -> {
                            existing.setShowActivity(showActivity);
                            ghostExceptionRepository.save(existing);
                        },
                        () -> ghostExceptionRepository.save(new GhostException(user, exceptionUser, showActivity))
                );
    }

    @Transactional
    public void removeException(String username, String exceptionUsername) {
        ghostExceptionRepository.deleteByUsernameAndExceptionUsername(username, exceptionUsername);
    }
}
