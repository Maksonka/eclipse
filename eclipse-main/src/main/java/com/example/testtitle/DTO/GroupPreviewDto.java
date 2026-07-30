package com.example.testtitle.DTO;

public class GroupPreviewDto {
    private Long groupId;
    private String groupName;
    private String lastMessagePreview;
    private String lastMessageTime;
    private String lastMessageSender;
    private long unreadCount;

    public GroupPreviewDto() {
    }

    public GroupPreviewDto(Long groupId, String groupName, String lastMessagePreview,
                           String lastMessageTime, String lastMessageSender, long unreadCount) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageTime = lastMessageTime;
        this.lastMessageSender = lastMessageSender;
        this.unreadCount = unreadCount;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLastMessageSender() {
        return lastMessageSender;
    }

    public void setLastMessageSender(String lastMessageSender) {
        this.lastMessageSender = lastMessageSender;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
