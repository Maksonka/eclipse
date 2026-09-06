package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.GhostException;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.GhostExceptionRepository;
import com.example.shadowvibe.Repositories.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GhostModeService {

    private final GhostExceptionRepository ghostExceptionRepository;
    private final UserService userService;
    private final MessageRepository messageRepository;

    public GhostModeService(GhostExceptionRepository ghostExceptionRepository,
                            UserService userService,
                            MessageRepository messageRepository) {
        this.ghostExceptionRepository = ghostExceptionRepository;
        this.userService = userService;
        this.messageRepository = messageRepository;
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
        // PREMIUM отключён: режим Призрака доступен всем
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

    public List<Map<String, Object>> getChatPartners(String username) {
        List<String> partnerNames = messageRepository.findConversationPartners(username);
        List<String> existing = ghostExceptionRepository.findByUsername(username).stream()
                .map(ge -> ge.getExceptionUser().getUsername())
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : partnerNames) {
            if (existing.contains(name) || name.equals(username)) {
                continue;
            }
            User partner = userService.findByUsername(name).orElse(null);
            if (partner == null) {
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("username", partner.getUsername());
            map.put("avatarFilename", partner.getAvatarFilename());
            result.add(map);
        }
        return result;
    }
}
