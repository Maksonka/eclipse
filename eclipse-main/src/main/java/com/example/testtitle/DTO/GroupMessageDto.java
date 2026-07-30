package com.example.testtitle.DTO;

public class GroupMessageDto {
    private Long id;
    private Long groupId;
    private String content;
    private String senderUsername;
    private String timestamp;

    public GroupMessageDto() {
    }

    public GroupMessageDto(Long id, Long groupId, String content, String senderUsername, String timestamp) {
        this.id = id;
        this.groupId = groupId;
        this.content = content;
        this.senderUsername = senderUsername;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
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
}
