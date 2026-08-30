package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.UserBlock;
import com.example.shadowvibe.Repositories.GroupMembershipRepository;
import com.example.shadowvibe.Repositories.MessageRepository;
import com.example.shadowvibe.Repositories.UserBlockRepository;
import com.example.shadowvibe.Repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    public BlockService(UserBlockRepository userBlockRepository,
                        UserRepository userRepository,
                        MessageRepository messageRepository,
                        GroupMembershipRepository groupMembershipRepository) {
        this.userBlockRepository = userBlockRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.groupMembershipRepository = groupMembershipRepository;
    }

    public boolean isBlocked(String blockerUsername, String targetUsername) {
        return userBlockRepository.existsByBlockerUsernameAndBlockedUsername(blockerUsername, targetUsername);
    }

    public boolean hasBlockBetween(String usernameA, String usernameB) {
        return isBlocked(usernameA, usernameB) || isBlocked(usernameB, usernameA);
    }

    @Transactional
    public void block(String blockerUsername, String targetUsername) {
        if (blockerUsername == null || targetUsername == null || blockerUsername.equals(targetUsername)) {
            throw new IllegalArgumentException("Нельзя заблокировать самого себя");
        }
        userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (!isBlocked(blockerUsername, targetUsername)) {
            userBlockRepository.save(new UserBlock(blockerUsername, targetUsername));
        }
    }

    @Transactional
    public void unblock(String blockerUsername, String targetUsername) {
        userBlockRepository.deleteByBlockerUsernameAndBlockedUsername(blockerUsername, targetUsername);
    }

    public List<String> getBlockedUsernames(String blockerUsername) {
        return userBlockRepository.findByBlockerUsernameOrderByCreatedAtDesc(blockerUsername).stream()
                .map(UserBlock::getBlockedUsername)
                .toList();
    }

    /**
     * Может ли sender отправить личное сообщение receiver?
     * Учитывает чёрный список в обе стороны и настройку «кто может писать мне».
     */
    public boolean canMessage(String senderUsername, String receiverUsername) {
        if (hasBlockBetween(senderUsername, receiverUsername)) {
            return false;
        }
        return userRepository.findByUsername(receiverUsername)
                .map(receiver -> {
                    if (receiver.getMessagesFrom() == com.example.shadowvibe.enums.MessagesFrom.KNOWN_ONLY) {
                        return hasExistingConnection(senderUsername, receiverUsername);
                    }
                    return true;
                })
                .orElse(false);
    }

    public void assertCanMessage(String senderUsername, String receiverUsername) {
        if (!canMessage(senderUsername, receiverUsername)) {
            throw new IllegalArgumentException("Пользователь недоступен для личных сообщений");
        }
    }

    public boolean hasExistingConnection(String usernameA, String usernameB) {
        if (messageRepository.existsConversationBetween(usernameA, usernameB)) {
            return true;
        }
        return groupMembershipRepository.countSharedGroups(usernameA, usernameB) > 0;
    }
}
