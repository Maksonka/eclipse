package com.example.testtitle.Models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_room_playlist_items")
public class WatchRoomPlaylistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_url", nullable = false, length = 2000)
    private String videoUrl;

    @Column(name = "title", length = 300)
    private String title;

    @Column(name = "added_by", nullable = false)
    private String addedBy;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private WatchRoom room;

    public WatchRoomPlaylistItem() {
    }

    public Long getId() {
        return id;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public WatchRoom getRoom() {
        return room;
    }

    public void setRoom(WatchRoom room) {
        this.room = room;
    }
}
