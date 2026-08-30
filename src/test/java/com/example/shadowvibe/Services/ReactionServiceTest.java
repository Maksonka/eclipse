package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.MessageReaction;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.GroupMessageRepository;
import com.example.shadowvibe.Repositories.MessageReactionRepository;
import com.example.shadowvibe.Repositories.MessageRepository;
import com.example.shadowvibe.Repositories.WatchRoomMessageRepository;
import com.example.shadowvibe.enums.ReactionTargetType;
import com.example.shadowvibe.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock
    private MessageReactionRepository reactionRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private GroupMessageRepository groupMessageRepository;
    @Mock
    private WatchRoomMessageRepository watchRoomMessageRepository;
    @Mock
    private UserService userService;
    @Mock
    private PremiumService premiumService;

    private ReactionService service;

    private final User alice = userWithId(1L, "alice");

    @BeforeEach
    void setUp() {
        service = new ReactionService(reactionRepository, messageRepository,
                groupMessageRepository, watchRoomMessageRepository, userService, premiumService);
    }

    private User userWithId(Long id, String username) {
        User u = new User(username, username + "@x.com", "p", UserRole.USER);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Message directMessageBetween(String sender, String receiver) {
        Message m = new Message();
        ReflectionTestUtils.setField(m, "id", 10L);
        m.setSender(new User(sender, "p"));
        m.setReceiver(new User(receiver, "p"));
        return m;
    }

    @Test
    void toggle_addsReactionAndReturnsUsernames() {
        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(messageRepository.findById(10L)).thenReturn(Optional.of(directMessageBetween("alice", "bob")));
        when(reactionRepository.deleteByTypeAndMessageAndUserAndEmoji(
                ReactionTargetType.DIRECT, 10L, 1L, "👍")).thenReturn(0);
        when(reactionRepository.save(any(MessageReaction.class))).thenAnswer(i -> i.getArgument(0));

        MessageReaction stored = new MessageReaction();
        stored.setMessageType(ReactionTargetType.DIRECT);
        stored.setMessageId(10L);
        stored.setUser(alice);
        stored.setEmoji("👍");
        when(reactionRepository.findByMessageTypeAndMessageId(ReactionTargetType.DIRECT, 10L))
                .thenReturn(List.of(stored));

        Map<String, List<String>> reactions = service.toggle(ReactionTargetType.DIRECT, 10L, "alice", "👍");

        verify(reactionRepository).save(any(MessageReaction.class));
        assertEquals(List.of("alice"), reactions.get("👍"));
    }

    @Test
    void toggle_removesExistingReaction() {
        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(messageRepository.findById(10L)).thenReturn(Optional.of(directMessageBetween("alice", "bob")));
        when(reactionRepository.deleteByTypeAndMessageAndUserAndEmoji(
                ReactionTargetType.DIRECT, 10L, 1L, "👍")).thenReturn(1);
        when(reactionRepository.findByMessageTypeAndMessageId(ReactionTargetType.DIRECT, 10L))
                .thenReturn(List.of());

        Map<String, List<String>> reactions = service.toggle(ReactionTargetType.DIRECT, 10L, "alice", "👍");

        verify(reactionRepository, never()).save(any(MessageReaction.class));
        assertTrue(reactions.isEmpty());
    }

    @Test
    void toggle_deniesNonParticipant() {
        when(userService.findByUsername("eve")).thenReturn(Optional.of(userWithId(2L, "eve")));
        when(messageRepository.findById(10L)).thenReturn(Optional.of(directMessageBetween("alice", "bob")));

        assertThrows(RuntimeException.class,
                () -> service.toggle(ReactionTargetType.DIRECT, 10L, "eve", "👍"));
        verify(reactionRepository, never()).save(any(MessageReaction.class));
    }

    @Test
    void toggle_rejectsInvalidEmoji() {
        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertThrows(RuntimeException.class,
                () -> service.toggle(ReactionTargetType.DIRECT, 10L, "alice", "a".repeat(30)));
        verify(reactionRepository, never()).save(any(MessageReaction.class));
    }

    @Test
    void getReactions_groupsByEmoji() {
        User bob = userWithId(2L, "bob");
        MessageReaction r1 = new MessageReaction();
        r1.setEmoji("👍");
        r1.setUser(alice);
        MessageReaction r2 = new MessageReaction();
        r2.setEmoji("👍");
        r2.setUser(bob);
        MessageReaction r3 = new MessageReaction();
        r3.setEmoji("❤️");
        r3.setUser(alice);

        when(reactionRepository.findByMessageTypeAndMessageId(ReactionTargetType.GROUP, 5L))
                .thenReturn(new ArrayList<>(List.of(r1, r2, r3)));

        Map<String, List<String>> reactions = service.getReactions(ReactionTargetType.GROUP, 5L);

        assertEquals(2, reactions.size());
        assertEquals(List.of("alice", "bob"), reactions.get("👍"));
        assertEquals(List.of("alice"), reactions.get("❤️"));
    }

    @Test
    void getReactionsBatch_groupsByMessage() {
        User bob = userWithId(2L, "bob");
        MessageReaction r1 = new MessageReaction();
        r1.setMessageId(1L);
        r1.setEmoji("👍");
        r1.setUser(alice);
        MessageReaction r2 = new MessageReaction();
        r2.setMessageId(2L);
        r2.setEmoji("🔥");
        r2.setUser(bob);

        when(reactionRepository.findByMessageTypeAndMessageIdIn(eq(ReactionTargetType.DIRECT), any()))
                .thenReturn(List.of(r1, r2));

        Map<Long, Map<String, List<String>>> batch =
                service.getReactionsBatch(ReactionTargetType.DIRECT, List.of(1L, 2L, 3L));

        assertEquals(List.of("alice"), batch.get(1L).get("👍"));
        assertEquals(List.of("bob"), batch.get(2L).get("🔥"));
        assertTrue(!batch.containsKey(3L));
    }

    @Test
    void getReactionsBatch_emptyIdsReturnsEmpty() {
        Map<Long, Map<String, List<String>>> batch =
                service.getReactionsBatch(ReactionTargetType.DIRECT, List.of());
        assertTrue(batch.isEmpty());
        verify(reactionRepository, never()).findByMessageTypeAndMessageIdIn(any(), any());
    }

    @Test
    void toggle_deniesForMissingMessage() {
        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.toggle(ReactionTargetType.DIRECT, 99L, "alice", "👍"));
    }

    @Test
    void assertMessageAccess_passesForGroupMember() {
        when(groupMessageRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> service.assertMessageAccess(ReactionTargetType.GROUP, 1L, "alice"));
    }
}
