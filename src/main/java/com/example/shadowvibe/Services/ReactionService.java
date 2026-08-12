package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.MessageReaction;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.GroupMessageRepository;
import com.example.shadowvibe.Repositories.MessageReactionRepository;
import com.example.shadowvibe.Repositories.MessageRepository;
import com.example.shadowvibe.Repositories.WatchRoomMessageRepository;
import com.example.shadowvibe.enums.ReactionTargetType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ReactionService {

    public static final List<String> BASE_EMOJIS = List.of(
            "\uD83D\uDC4D", "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE0E", "\uD83D\uDE22", "\uD83D\uDE00", "\uD83D\uDE0D", "\uD83D\uDEA9"
    );

    private final MessageReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final WatchRoomMessageRepository watchRoomMessageRepository;
    private final UserService userService;

    public ReactionService(MessageReactionRepository reactionRepository,
                           MessageRepository messageRepository,
                           GroupMessageRepository groupMessageRepository,
                           WatchRoomMessageRepository watchRoomMessageRepository,
                           UserService userService) {
        this.reactionRepository = reactionRepository;
        this.messageRepository = messageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.watchRoomMessageRepository = watchRoomMessageRepository;
        this.userService = userService;
    }

    public Map<String, List<String>> toggle(ReactionTargetType type, Long messageId, String username, String emoji) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (emoji == null || emoji.isBlank() || emoji.length() > 16) {
            throw new RuntimeException("Некорректный эмодзи");
        }
        assertMessageAccess(type, messageId, username);

        int removed = reactionRepository.deleteByTypeAndMessageAndUserAndEmoji(type, messageId, user.getId(), emoji);
        if (removed == 0) {
            MessageReaction reaction = new MessageReaction();
            reaction.setMessageType(type);
            reaction.setMessageId(messageId);
            reaction.setUser(user);
            reaction.setEmoji(emoji);
            reaction.setCreatedAt(LocalDateTime.now());
            reactionRepository.save(reaction);
        }
        return getReactions(type, messageId);
    }

    public Map<String, List<String>> getReactions(ReactionTargetType type, Long messageId) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (MessageReaction reaction : reactionRepository.findByMessageTypeAndMessageId(type, messageId)) {
            result.computeIfAbsent(reaction.getEmoji(), k -> new ArrayList<>())
                    .add(reaction.getUser().getUsername());
        }
        return result;
    }

    public Map<Long, Map<String, List<String>>> getReactionsBatch(ReactionTargetType type, List<Long> messageIds) {
        Map<Long, Map<String, List<String>>> result = new LinkedHashMap<>();
        if (messageIds == null || messageIds.isEmpty()) {
            return result;
        }
        for (MessageReaction reaction : reactionRepository.findByMessageTypeAndMessageIdIn(type, messageIds)) {
            result.computeIfAbsent(reaction.getMessageId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(reaction.getEmoji(), k -> new ArrayList<>())
                    .add(reaction.getUser().getUsername());
        }
        return result;
    }

    public void assertMessageAccess(ReactionTargetType type, Long messageId, String username) {
        switch (type) {
            case DIRECT -> {
                Message message = messageRepository.findById(messageId)
                        .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));
                if (!message.getSender().getUsername().equals(username)
                        && !message.getReceiver().getUsername().equals(username)) {
                    throw new RuntimeException("Нет доступа к сообщению");
                }
            }
            case GROUP -> {
                if (groupMessageRepository.findById(messageId)
                        .map(m -> m.getGroup().isMember(username))
                        .orElseThrow(() -> new RuntimeException("Сообщение не найдено")) == Boolean.FALSE) {
                    throw new RuntimeException("Нет доступа к сообщению");
                }
            }
            case WATCH -> {
                if (watchRoomMessageRepository.findById(messageId)
                        .map(m -> m.getRoom().isMember(username))
                        .orElseThrow(() -> new RuntimeException("Сообщение не найдено")) == Boolean.FALSE) {
                    throw new RuntimeException("Нет доступа к сообщению");
                }
            }
        }
    }
}
