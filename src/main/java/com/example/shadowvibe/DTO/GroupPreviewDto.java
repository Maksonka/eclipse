package com.example.shadowvibe.DTO;

public class GroupPreviewDto {
    private Long groupId;
    private String groupName;
    private String avatarFilename;
    private String lastMessagePreview;
    private String lastMessageTime;
    private String lastMessageSender;
    private long unreadCount;
    private boolean muted;

    public GroupPreviewDto() {
    }

    public GroupPreviewDto(Long groupId, String groupName, String avatarFilename, String lastMessagePreview,
                           String lastMessageTime, String lastMessageSender, long unreadCount) {
        this(groupId, groupName, avatarFilename, lastMessagePreview, lastMessageTime, lastMessageSender, unreadCount, false);
    }

    public GroupPreviewDto(Long groupId, String groupName, String avatarFilename, String lastMessagePreview,
                           String lastMessageTime, String lastMessageSender, long unreadCount, boolean muted) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.avatarFilename = avatarFilename;
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageTime = lastMessageTime;
        this.lastMessageSender = lastMessageSender;
        this.unreadCount = unreadCount;
        this.muted = muted;
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

    public String getAvatarFilename() {
        return avatarFilename;
    }

    public void setAvatarFilename(String avatarFilename) {
        this.avatarFilename = avatarFilename;
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

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }
}
