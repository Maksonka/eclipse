package com.example.shadowvibe.Models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_sticker_packs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "pack_id"})
)
public class UserStickerPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pack_id", nullable = false)
    private StickerPack pack;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UserStickerPack() {
    }

    public UserStickerPack(User user, StickerPack pack) {
        this.user = user;
        this.pack = pack;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public StickerPack getPack() {
        return pack;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
