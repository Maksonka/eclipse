package com.example.shadowvibe.Models;

import com.example.shadowvibe.enums.ReactionTargetType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_reactions", uniqueConstraints = {
        @UniqueConstraint(name = "message_reactions_uk",
                columnNames = {"message_type", "message_id", "user_id", "emoji"})
})
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ReactionTargetType messageType;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "emoji", nullable = false, length = 32)
    private String emoji;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MessageReaction() {
    }

    public Long getId() {
        return id;
    }

    public ReactionTargetType getMessageType() {
        return messageType;
    }

    public void setMessageType(ReactionTargetType messageType) {
        this.messageType = messageType;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
