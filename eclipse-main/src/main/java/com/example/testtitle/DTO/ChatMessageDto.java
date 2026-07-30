package com.example.testtitle.DTO;

public class ChatMessageDto {
    private Long id;
    private String content;
    private String senderUsername;
    private String receiverUsername;
    private String timestamp;
    private boolean read;

    public ChatMessageDto() {
    }

    public ChatMessageDto(Long id, String content, String senderUsername, String receiverUsername, String timestamp, boolean read) {
        this.id = id;
        this.content = content;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.timestamp = timestamp;
        this.read = read;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
