package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.MessageReaction;
import com.example.shadowvibe.enums.ReactionTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    List<MessageReaction> findByMessageTypeAndMessageId(ReactionTargetType messageType, Long messageId);

    List<MessageReaction> findByMessageTypeAndMessageIdIn(ReactionTargetType messageType, Collection<Long> messageIds);

    Optional<MessageReaction> findByMessageTypeAndMessageIdAndUserId(
            ReactionTargetType messageType, Long messageId, Long userId);

    @Modifying
    @Query("DELETE FROM MessageReaction r WHERE r.messageType = :type AND r.messageId = :messageId AND r.user.id = :userId AND r.emoji = :emoji")
    int deleteByTypeAndMessageAndUserAndEmoji(
            @Param("type") ReactionTargetType type,
            @Param("messageId") Long messageId,
            @Param("userId") Long userId,
            @Param("emoji") String emoji);
}
