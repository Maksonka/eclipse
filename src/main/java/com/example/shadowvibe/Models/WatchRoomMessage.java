package com.example.shadowvibe.Models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_room_messages")
public class WatchRoomMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "sticker_code", length = 80)
    private String stickerCode;

    @Column(name = "sticker_url", length = 255)
    private String stickerUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private WatchRoom room;

    public WatchRoomMessage() {
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public WatchRoom getRoom() {
        return room;
    }

    public void setRoom(WatchRoom room) {
        this.room = room;
    }

    public String getStickerCode() { return stickerCode; }
    public void setStickerCode(String stickerCode) { this.stickerCode = stickerCode; }

    public String getStickerUrl() { return stickerUrl; }
    public void setStickerUrl(String stickerUrl) { this.stickerUrl = stickerUrl; }

    public boolean hasSticker() { return stickerUrl != null && !stickerUrl.isBlank(); }
}
