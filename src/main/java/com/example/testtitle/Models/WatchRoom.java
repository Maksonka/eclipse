package com.example.testtitle.Models;

import com.example.testtitle.enums.WatchRoomStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "watch_rooms")
public class WatchRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false, unique = true, length = 12)
    private String roomCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "host_username", nullable = false)
    private String hostUsername;

    @Column(name = "video_url", length = 2000)
    private String videoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WatchRoomStatus status = WatchRoomStatus.IDLE;

    @Column(name = "position_ms", nullable = false)
    private long positionMs;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "current_item_id")
    private Long currentItemId;

    @ManyToMany
    @JoinTable(
            name = "watch_room_members",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();

    public WatchRoom() {
    }

    public WatchRoom(String roomCode, String name, User host) {
        this.roomCode = roomCode;
        this.name = name;
        this.hostUsername = host.getUsername();
        this.status = WatchRoomStatus.IDLE;
        this.positionMs = 0;
        this.updatedAt = Instant.now();
        this.createdAt = LocalDateTime.now();
        this.members.add(host);
    }

    public Long getId() {
        return id;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public WatchRoomStatus getStatus() {
        return status;
    }

    public void setStatus(WatchRoomStatus status) {
        this.status = status;
    }

    public long getPositionMs() {
        return positionMs;
    }

    public void setPositionMs(long positionMs) {
        this.positionMs = positionMs;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getCurrentItemId() {
        return currentItemId;
    }

    public void setCurrentItemId(Long currentItemId) {
        this.currentItemId = currentItemId;
    }

    public Set<User> getMembers() {
        return members;
    }

    public void setMembers(Set<User> members) {
        this.members = members;
    }

    public boolean isMember(String username) {
        return members.stream().anyMatch(m -> m.getUsername().equals(username));
    }
}
