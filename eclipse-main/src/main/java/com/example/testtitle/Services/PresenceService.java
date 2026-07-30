package com.example.testtitle.Services;

import com.example.testtitle.DTO.ChatReadReceiptDto;
import com.example.testtitle.Models.Message;
import com.example.testtitle.Models.User;
import com.example.testtitle.Repositories.MessageRepository;
import com.example.testtitle.Repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceService(MessageRepository messageRepository, SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
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
        return partners;
    }

    private void broadcastPresence(String username, boolean online) {
        List<String> partners = messageRepository.findConversationPartners(username);
        for (String partner : partners) {
            messagingTemplate.convertAndSendToUser(
                    partner,
                    "/queue/presence",
                    Map.of("username", username, "online", online)
            );
        }
    }
}
