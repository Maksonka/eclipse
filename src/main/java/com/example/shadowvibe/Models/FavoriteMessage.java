package com.example.shadowvibe.Models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "favorite_messages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_type", "message_id"})
)
public class FavoriteMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "target_type", nullable = false, length = 8)
    private String targetType;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "favorited_at", nullable = false)
    private LocalDateTime favoritedAt = LocalDateTime.now();

    public FavoriteMessage() {
    }

    public FavoriteMessage(User user, String targetType, Long messageId) {
        this.user = user;
        this.targetType = targetType;
        this.messageId = messageId;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public LocalDateTime getFavoritedAt() {
        return favoritedAt;
    }

    public void setFavoritedAt(LocalDateTime favoritedAt) {
        this.favoritedAt = favoritedAt;
    }
}
