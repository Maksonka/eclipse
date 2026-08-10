package com.example.shadowvibe.DTO;

public class WatchRoomChatMessageDto {
    private Long messageId;
    private Long roomId;
    private String content;
    private String senderUsername;
    private String timestamp;
    private String stickerCode;
    private String stickerUrl;
    private String audioUrl;

    public WatchRoomChatMessageDto() {
    }

    public WatchRoomChatMessageDto(Long messageId, Long roomId, String content,
                                   String senderUsername, String timestamp) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.content = content;
        this.senderUsername = senderUsername;
        this.timestamp = timestamp;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStickerCode() { return stickerCode; }
    public void setStickerCode(String stickerCode) { this.stickerCode = stickerCode; }

    public String getStickerUrl() { return stickerUrl; }
    public void setStickerUrl(String stickerUrl) { this.stickerUrl = stickerUrl; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
}
