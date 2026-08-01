package com.example.testtitle.DTO;

public class ChatTypingDto {
    private String senderUsername;
    private boolean typing;

    public ChatTypingDto() {
    }

    public ChatTypingDto(String senderUsername, boolean typing) {
        this.senderUsername = senderUsername;
        this.typing = typing;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }
}
