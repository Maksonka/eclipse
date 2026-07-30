package com.example.testtitle.Models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "content")
    private String content;
    @Column(name = "time")
    private LocalDateTime timestamp;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;


    public Message(Long id, String content, LocalDateTime timestamp, User sender, User receiver) {
        this.id = id;
        this.content = content;
        this.timestamp = timestamp;
        this.sender = sender;
        this.receiver = receiver;
    }

    public Message() {
    }


    public Long getId() {
        return id;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
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


    public User getSender() {
        return sender;
    }


    public User getRecipient() {
        return receiver;
    }

    public User getReceiver() {
        return receiver;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public boolean isRead() {
        return readAt != null;
    }
}
