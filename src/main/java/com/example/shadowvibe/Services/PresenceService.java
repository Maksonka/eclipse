package com.example.shadowvibe.Services;

import com.example.shadowvibe.DTO.ChatReadReceiptDto;
import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.MessageRepository;
import com.example.shadowvibe.Repositories.UserBlockRepository;
import com.example.shadowvibe.Repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;

    public PresenceService(MessageRepository messageRepository,
                           SimpMessagingTemplate messagingTemplate,
                           UserRepository userRepository,
                           UserBlockRepository userBlockRepository) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.userBlockRepository = userBlockRepository;
    }

    public void userConnected(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        onlineUsers.add(username);
        broadcastPresence(username, true);
    }

    public void userDisconnected(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        onlineUsers.remove(username);
        broadcastPresence(username, false);
    }

    public boolean isOnline(String username) {
        return onlineUsers.contains(username);
    }

    public Set<String> getOnlineUsers() {
        return Set.copyOf(onlineUsers);
    }

    public Set<String> getOnlinePartners(String username) {
        Set<String> partners = new HashSet<>(messageRepository.findConversationPartners(username));
        partners.retainAll(onlineUsers);
        partners.removeIf(partner -> !isOnlineVisibleTo(username, partner));
        return partners;
    }

    /**
     * Виден ли статус «в сети» пользователя target для viewer?
     * Учитывает настройку «скрывать онлайн» и чёрный список в обе стороны.
     */
    public boolean isOnlineVisibleTo(String viewerUsername, String targetUsername) {
        if (viewerUsername == null || targetUsername == null || viewerUsername.equals(targetUsername)) {
            return isOnline(targetUsername);
        }
        if (!isOnline(targetUsername)) {
            return false;
        }
        Optional<User> target = userRepository.findByUsername(targetUsername);
        if (target.isPresent() && target.get().isHideOnlineStatus()) {
            return false;
        }
        return !userBlockRepository.existsByBlockerUsernameAndBlockedUsername(viewerUsername, targetUsername)
                && !userBlockRepository.existsByBlockerUsernameAndBlockedUsername(targetUsername, viewerUsername);
    }

    private void broadcastPresence(String username, boolean online) {
        List<String> partners = messageRepository.findConversationPartners(username);
        for (String partner : partners) {
            if (!canReceivePresence(username, partner)) {
                continue;
            }
            messagingTemplate.convertAndSendToUser(
                    partner,
                    "/queue/presence",
                    Map.of("username", username, "online", online)
            );
        }
    }

    private boolean canReceivePresence(String username, String partner) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && user.get().isHideOnlineStatus()) {
            return false;
        }
        return !userBlockRepository.existsByBlockerUsernameAndBlockedUsername(username, partner)
                && !userBlockRepository.existsByBlockerUsernameAndBlockedUsername(partner, username);
    }
}
