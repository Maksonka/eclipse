package com.example.shadowvibe.DTO;

public class WatchRoomReactionDto {
    private Long roomId;
    private String username;
    private String emoji;
    private long timestamp;

    public WatchRoomReactionDto() {
    }

    public WatchRoomReactionDto(Long roomId, String username, String emoji, long timestamp) {
        this.roomId = roomId;
        this.username = username;
        this.emoji = emoji;
        this.timestamp = timestamp;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
