package com.example.shadowvibe.Models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_blocks",
       uniqueConstraints = @UniqueConstraint(name = "uq_user_blocks_pair",
               columnNames = {"blocker_username", "blocked_username"}))
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "blocker_username", nullable = false)
    private String blockerUsername;

    @Column(name = "blocked_username", nullable = false)
    private String blockedUsername;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UserBlock() {
    }

    public UserBlock(String blockerUsername, String blockedUsername) {
        this.blockerUsername = blockerUsername;
        this.blockedUsername = blockedUsername;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getBlockerUsername() {
        return blockerUsername;
    }

    public void setBlockerUsername(String blockerUsername) {
        this.blockerUsername = blockerUsername;
    }

    public String getBlockedUsername() {
        return blockedUsername;
    }

    public void setBlockedUsername(String blockedUsername) {
        this.blockedUsername = blockedUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
